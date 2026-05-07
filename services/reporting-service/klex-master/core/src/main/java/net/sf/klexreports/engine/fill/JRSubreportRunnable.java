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

import org.apache.commons.javaflow.api.continuable;

/**
 * Abstract base for {@link java.lang.Runnable Runnable}-based
 * {@link net.sf.klexreports.engine.fill.JRSubreportRunner JRSubreportRunner}
 * implementations.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class JRSubreportRunnable implements Runnable
{
	private final JRFillSubreport fillSubreport;
	
	private Throwable error;
	private boolean running;
	
	protected JRSubreportRunnable(JRFillSubreport fillSubreport)
	{
		this.fillSubreport = fillSubreport;
	}

	protected JRSubreportRunResult runResult()
	{
		return new JRSubreportRunResult(!running, error);
	}
	
	@Override
	@continuable
	public void run()
	{
		running = true;		
		error = null;
		
		try
		{
			fillSubreport.fillSubreport();
		}
		catch (JRFillInterruptedException e)
		{
			//If the subreport filler was interrupted, we should remain silent
		}
		// we have to catch Throwable, because it is difficult to say what would happen with the master
		// filler thread in case we don't
		catch (Throwable t) //NOPMD
		{
			error = t;
		}

		running = false;
	}
	
	protected boolean isRunning()
	{
		return running;
	}
}
