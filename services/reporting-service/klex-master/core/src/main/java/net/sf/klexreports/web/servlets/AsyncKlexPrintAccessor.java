/*
 * KlexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from Klexsoft,
 * the following license terms apply:
 *
 * This program is part of KlexReports.
 *
 * KlexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KlexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with KlexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.web.servlets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.fill.AsynchronousFilllListener;
import net.sf.klexreports.engine.fill.FillHandle;
import net.sf.klexreports.engine.fill.FillListener;

/**
 * Generated report accessor used for asynchronous report executions that publishes pages
 * before the entire report has been generated.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class AsyncKlexPrintAccessor implements KlexPrintAccessor, AsynchronousFilllListener, FillListener
{

	private static final Log log = LogFactory.getLog(AsyncKlexPrintAccessor.class);
	public static final String EXCEPTION_MESSAGE_KEY_LOCK_ATTEMPT_INTERRUPTED = "web.servlets.lock.attempt.interrupted";
	public static final String EXCEPTION_MESSAGE_KEY_NO_KLEXPRINT_GENERATED = "web.servlets.no.klexprint.generated";
	public static final String EXCEPTION_MESSAGE_KEY_REPORT_GENERATION_CANCELLED = "web.servlets.report.generation.cancelled";
	public static final String EXCEPTION_MESSAGE_KEY_ASYNC_REPORT_GENERATION_ERROR = "web.servlets.async.report.generation.error";
	
	private FillHandle fillHandle;
	private final Lock lock;
	private final Condition pageCondition;
	private final Map<Integer, Long> trackedPages = new HashMap<>();
	
	private volatile boolean done;
	private boolean cancelled;
	private Throwable error;
	private volatile KlexPrint klexPrint;
	private int pageCount;
	
	/**
	 * Create a report accessor.
	 * 
	 * @param fillHandle the asynchronous fill handle used by this accessor
	 */
	public AsyncKlexPrintAccessor(FillHandle fillHandle)
	{
		this.fillHandle = fillHandle;
		lock = new ReentrantLock(true);
		pageCondition = lock.newCondition();
		
		fillHandle.addListener(this);
		fillHandle.addFillListener(this);
	}

	protected void lock()
	{
		try
		{
			lock.lockInterruptibly();
		}
		catch (InterruptedException e)
		{
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_LOCK_ATTEMPT_INTERRUPTED,
					(Object[])null,
					e);
		}
	}

	protected void unlock()
	{
		lock.unlock();
	}
	
	@Override
	public ReportPageStatus pageStatus(int pageIdx, Long pageTimestamp)
	{
		if (!done)
		{
			lock();
			try
			{
				// wait until the page is available
				while (!done && pageIdx >= pageCount)
				{
					if (log.isDebugEnabled())
					{
						log.debug("waiting for page " + pageIdx);
					}
					
					pageCondition.await();
				}
			}
			catch (InterruptedException e)
			{
				throw new JRRuntimeException(e);
			}
			finally
			{
				unlock();
			}
		}
		
		if (pageIdx >= pageCount)
		{
			return ReportPageStatus.NO_SUCH_PAGE;
		}
		
		if (done || fillHandle.isPageFinal(pageIdx))
		{
			trackedPages.remove(pageIdx);
			return ReportPageStatus.PAGE_FINAL;
		}
		
		long timestamp;
		boolean modified;
		
		Long lastUpdate = trackedPages.get(pageIdx);
		if (lastUpdate == null)
		{
			// we don't know when exactly the page was modified, using current time
			timestamp = System.currentTimeMillis();
			modified = true;
		}
		else
		{
			timestamp = lastUpdate;
			modified = pageTimestamp == null || pageTimestamp < lastUpdate;
		}
		
		ReportPageStatus status = ReportPageStatus.nonFinal(timestamp, modified);
		// add the page to the tracked map so that we catch updates
		trackedPages.put(pageIdx, timestamp);
		return status;
	}

	@Override
	public KlexPrint getKlexPrint()
	{
		return klexPrint;
	}

	public boolean waitForFinalKlexPrint(int milliseconds)
	{
		if (!done)
		{
			lock();
			try
			{
				long waitNanos = TimeUnit.MILLISECONDS.toNanos(milliseconds);
				// wait until the report generation is done or the time expires
				while (!done && waitNanos > 0)
				{
					if (log.isDebugEnabled())
					{
						log.debug("waiting for report end");
					}
					
					//FIXME use a condition dedicated to report completion
					waitNanos = pageCondition.awaitNanos(waitNanos);
				}
			}
			catch (InterruptedException e)
			{
				log.error("Error while waiting for final KlexPrint", e);
				return false;
			}
			finally
			{
				unlock();
			}
		}
		
		return done;
	}
	
	@Override
	public KlexPrint getFinalKlexPrint()
	{
		if (!done)
		{
			lock();
			try
			{
				// wait until the report generation is done
				while (!done)
				{
					if (log.isDebugEnabled())
					{
						log.debug("waiting for report end");
					}
					
					pageCondition.await();//FIXME use a different condition to void frequent interruptions
				}
			}
			catch (InterruptedException e)
			{
				throw new JRRuntimeException(e);
			}
			finally
			{
				unlock();
			}
		}
		
		if (error != null)
		{
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_ASYNC_REPORT_GENERATION_ERROR,
					(Object[])null,
					error);
		}
		
		if (klexPrint == null)
		{
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_NO_KLEXPRINT_GENERATED,
					(Object[])null);
		}
		
		return klexPrint;
	}

	@Override
	public void reportFinished(KlexPrint klexPrint)
	{
		if (log.isDebugEnabled())
		{
			log.debug("report finished");
		}
		
		lock();
		try
		{
			if (this.klexPrint == null)
			{
				this.klexPrint = klexPrint;
			}
			
			pageCount = klexPrint.getPages().size();
			done = true;
			
			// clear fillHandle to release filler references
			fillHandle = null;
			trackedPages.clear();
			
			pageCondition.signalAll();
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public void reportCancelled()
	{
		if (log.isDebugEnabled())
		{
			log.debug("report cancelled");
		}
		
		lock();
		try
		{
			cancelled = true;
			done = true;
			pageCount = klexPrint == null ? 0 : klexPrint.getPages().size();

			// store an error as cancelled status
			error = new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_REPORT_GENERATION_CANCELLED,
						(Object[])null);
			
			// clear fillHandle to release filler references
			fillHandle = null;
			
			// signal to pageStatus
			pageCondition.signalAll();
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public void reportFillError(Throwable t)
	{
		log.error("Error during report execution", t);
		
		lock();
		try
		{
			error = t;
			done = true;
			pageCount = klexPrint == null ? 0 : klexPrint.getPages().size();
			
			// clear fillHandle to release filler references
			fillHandle = null;
			
			// signal to pageStatus
			pageCondition.signalAll();
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public void pageGenerated(KlexPrint klexPrint, int pageIndex)
	{
		if (log.isDebugEnabled())
		{
			log.debug("page " + pageIndex + " generated");
		}
		
		lock();
		try
		{
			if (this.klexPrint == null)
			{
				this.klexPrint = klexPrint;
			}
			
			pageCount = pageIndex + 1;
			
			pageCondition.signalAll();
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public void pageUpdated(KlexPrint klexPrint, int pageIndex)
	{
		if (log.isDebugEnabled())
		{
			log.debug("page " + pageIndex + " updated");
		}
		
		lock();
		try
		{
			// update the timestamp if the page is tracked
			if (trackedPages.containsKey(pageIndex))
			{
				long timestamp = System.currentTimeMillis();
				trackedPages.put(pageIndex, timestamp);
			}
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public ReportExecutionStatus getReportStatus()
	{
		if (!done)
		{
			return ReportExecutionStatus.running(pageCount);
		}
		
		if (cancelled)
		{
			return ReportExecutionStatus.canceled(pageCount);
		}
		
		if (error != null)
		{
			return ReportExecutionStatus.error(pageCount, error);
		}
		
		return ReportExecutionStatus.finished(klexPrint.getPages().size());
	}

}
