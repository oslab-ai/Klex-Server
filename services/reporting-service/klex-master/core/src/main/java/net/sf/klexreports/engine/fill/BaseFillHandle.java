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
package net.sf.klexreports.engine.fill;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.KlexReportsContext;

/**
 * Base class used to perform report filling asychronously.
 * <p>
 * An instance of this type can be used as a handle to an asychronous fill process.
 * The main benefit of this method is that the filling process can be cancelled.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class BaseFillHandle implements FillHandle
{
	
	private static final Log log = LogFactory.getLog(BaseFillHandle.class);
	
	protected final KlexReportsContext klexReportsContext;
	protected final KlexReport klexReport;
	protected final Map<String,Object> parameters;
	protected final JRDataSource dataSource;
	protected final Connection conn;
	protected final ReportFiller filler;
	protected final List<AsynchronousFilllListener> listeners;
	protected boolean started;
	protected boolean running;
	protected boolean cancelled;
	protected final Object lock;
	
	protected BaseFillHandle (
			KlexReportsContext klexReportsContext,
			KlexReport klexReport,
			Map<String,Object> parameters,
			JRDataSource dataSource,
			Connection conn
			) throws JRException
	{
		this(klexReportsContext, SimpleKlexReportSource.from(klexReport),
				parameters, dataSource, conn);
	}
	
	protected BaseFillHandle (
			KlexReportsContext klexReportsContext,
			KlexReportSource reportSource,
			Map<String,Object> parameters,
			JRDataSource dataSource,
			Connection conn
			) throws JRException
	{
		this.klexReportsContext = klexReportsContext;
		this.klexReport = reportSource.getReport();
		this.parameters = parameters;
		this.dataSource = dataSource;
		this.conn = conn;
		this.filler = JRFiller.createReportFiller(klexReportsContext, reportSource);
		this.listeners = new ArrayList<>();
		lock = this;
	}

	
	@Override
	public void addListener(AsynchronousFilllListener listener)
	{
		listeners.add(listener);
	}

	@Override
	public void addFillListener(FillListener listener)
	{
		filler.addFillListener(listener);
	}


	@Override
	public boolean removeListener(AsynchronousFilllListener listener)
	{
		return listeners.remove(listener);
	}

	
	protected class ReportFill implements Runnable
	{
		@Override
		public void run()
		{
			synchronized (lock)
			{
				running = true;
			}
			
			try
			{
				KlexPrint print;
				if (conn != null)
				{
					print = filler.fill(parameters, conn);
				}
				else if (dataSource != null)
				{
					print = filler.fill(parameters, dataSource);
				}
				else
				{
					print = filler.fill(parameters);
				}
				
				notifyFinish(print);
			}
			catch (Throwable e) //NOPMD
			{
				if (log.isDebugEnabled())
				{
					log.debug("fill error", e);
				}
				
				synchronized (lock)
				{
					if (cancelled)
					{
						notifyCancel();
					}
					else
					{
						notifyError(e);
					}
				}
			}
			finally
			{
				synchronized (lock)
				{
					running = false;
				}
			}
		}
	}
	
	
	@Override
	public void startFill()
	{
		synchronized (lock)
		{
			if (started)
			{
				throw new IllegalStateException("Fill already started.");
			}

			started = true;
		}
		
		ReportFill reportFiller = new ReportFill();
		
		Executor executor = getReportExecutor();
		executor.execute(reportFiller);
	}
	
	protected abstract Executor getReportExecutor();
	
	
	@Override
	public void cancellFill() throws JRException
	{
		synchronized (lock)
		{
			if (!running)
			{
				throw new IllegalStateException("Fill not running.");
			}
			
			filler.cancelFill();
			cancelled = true;
		}
	}
	
	
	protected void notifyFinish(KlexPrint print)
	{
		for (Iterator<AsynchronousFilllListener> i = listeners.iterator(); i.hasNext();)
		{
			AsynchronousFilllListener listener = i.next();
			listener.reportFinished(print);
		}
	}
	
	
	protected void notifyCancel()
	{
		for (Iterator<AsynchronousFilllListener> i = listeners.iterator(); i.hasNext();)
		{
			AsynchronousFilllListener listener =  i.next();
			listener.reportCancelled();
		}
	}
	
	
	protected void notifyError(Throwable e)
	{
		for (Iterator<AsynchronousFilllListener> i = listeners.iterator(); i.hasNext();)
		{
			AsynchronousFilllListener listener = i.next();
			listener.reportFillError(e);
		}
	}
	
	@Override
	public boolean isPageFinal(int pageIdx)
	{
		return filler.isPageFinal(pageIdx);
	}
}
