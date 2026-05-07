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
package net.sf.klexreports.engine;

/**
 * Virtualization helper class.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public final class JRVirtualizationHelper
{
	private static final ThreadLocal<JRVirtualizer> threadVirtualizer = new ThreadLocal<>();
	private static final ThreadLocal<KlexReportsContext> threadKlexReportsContext = new ThreadLocal<>();

	
	/**
	 * Sets a virtualizer to be used for the current thread.
	 * <p>
	 * The current thread's virtualizer is used when a report obtained by virtualization
	 * is deserialized.
	 * 
	 * @param virtualizer
	 */
	public static void setThreadVirtualizer(JRVirtualizer virtualizer)
	{
		threadVirtualizer.set(virtualizer);
	}

	
	/**
	 * Clears the virtualizer associated to the current thread.
	 */
	public static void clearThreadVirtualizer()
	{
		threadVirtualizer.remove();
	}

	
	/**
	 * Returns the virtualizer associated to the current thread.
	 * <p>
	 * This method is used by {@link net.sf.klexreports.engine.base.JRVirtualPrintPage JRVirtualPrintPage}
	 * on deserialization.
	 * 
	 * @return the virtualizer associated to the current thread
	 */
	public static JRVirtualizer getThreadVirtualizer()
	{
		return threadVirtualizer.get();
	}
	
	
	/**
	 * Sets a KlexReportsContext to be used for the current thread.
	 * <p>
	 * The current thread's context is used when a report obtained by virtualization
	 * is deserialized.
	 * 
	 * @param klexReportsContext
	 */
	public static void setThreadKlexReportsContext(KlexReportsContext klexReportsContext)
	{
		threadKlexReportsContext.set(klexReportsContext);
	}

	
	/**
	 * Clears the KlexReportsContext associated to the current thread.
	 */
	public static void clearThreadKlexReportsContext()
	{
		threadKlexReportsContext.remove();
	}

	
	/**
	 * Returns the KlexReportsContext associated to the current thread.
	 * <p>
	 * This method is used by {@link net.sf.klexreports.engine.base.JRVirtualPrintPage JRVirtualPrintPage}
	 * on deserialization.
	 * 
	 * @return the KlexReportsContext associated to the current thread
	 */
	public static KlexReportsContext getThreadKlexReportsContext()
	{
		return threadKlexReportsContext.get();
	}
	
	
	private JRVirtualizationHelper()
	{
	}
}
