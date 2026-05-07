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
import java.util.Map;
import java.util.concurrent.Executor;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * Class used to perform report filling asychronously.
 * <p>
 * An instance of this type can be used as a handle to an asychronous fill process.
 * The main benefit of this method is that the filling process can be cancelled.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class AsynchronousFillHandle extends BaseFillHandle
{	
	
	/**
	 * A property that determines whether a report can be generated and displayed asynchronously in a viewer.
	 * 
	 * Asynchronous report generation implies displaying report pages before the report is complete.
	 */
	// TODO lucianc use in web viewer
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT},
			sinceVersion = PropertyConstants.VERSION_4_6_0,
			valueType = Boolean.class,
			defaultValue = PropertyConstants.BOOLEAN_TRUE
			)
	public static final String PROPERTY_REPORT_ASYNC = JRPropertiesUtil.PROPERTY_PREFIX + "viewer.async";
	
	protected Thread fillThread;
	protected Integer priority;
	protected String threadName;
	
	protected AsynchronousFillHandle (
		KlexReportsContext klexReportsContext,
		KlexReport klexReport,
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		this(klexReportsContext, klexReport, parameters, dataSource, null);
	}
	
	protected AsynchronousFillHandle (
		KlexReportsContext klexReportsContext,
		KlexReport klexReport,
		Map<String,Object> parameters,
		Connection conn
		) throws JRException
	{
		this(klexReportsContext, klexReport, parameters, null, conn);
	}
	
	protected AsynchronousFillHandle (
		KlexReportsContext klexReportsContext,
		KlexReport klexReport,
		Map<String,Object> parameters
		) throws JRException
	{
		this(klexReportsContext, klexReport, parameters, null, null);
	}
	
	protected AsynchronousFillHandle (
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
	
	
	protected AsynchronousFillHandle (
		KlexReportsContext klexReportsContext,
		KlexReportSource reportSource,
		Map<String,Object> parameters,
		JRDataSource dataSource,
		Connection conn
		) throws JRException
	{
		super(klexReportsContext, reportSource, parameters, dataSource, conn);
	}
	
	/**
	 * Returns an executor that creates a new thread to perform the report execution.
	 */
	@Override
	protected Executor getReportExecutor()
	{
		return new ThreadExecutor();
	}

	protected class ThreadExecutor implements Executor
	{
		@Override
		public void execute(Runnable command)
		{
			if (threadName == null)
			{
				fillThread = new Thread(command);
			}
			else
			{
				fillThread = new Thread(command, threadName);
			}
			
			if (priority != null)
			{
				fillThread.setPriority(priority);
			}
			
			fillThread.start();
		}
	}

	/**
	 * Creates an asychronous filling handle.
	 * 
	 * @param klexReportsContext the context
	 * @param klexReport the report
	 * @param parameters the parameter map
	 * @param dataSource the data source
	 * @return the handle
	 * @throws JRException
	 */
	public static AsynchronousFillHandle createHandle(
		KlexReportsContext klexReportsContext,
		KlexReport klexReport,
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		return createHandle(klexReportsContext, SimpleKlexReportSource.from(klexReport),
				parameters, dataSource);
	}
	
	public static AsynchronousFillHandle createHandle(
		KlexReportsContext klexReportsContext,
		KlexReportSource reportSource,
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		return new AsynchronousFillHandle(klexReportsContext, reportSource, parameters, dataSource, null);
	}


	/**
	 * Creates an asychronous filling handle.
	 * 
	 * @param klexReportsContext the context
	 * @param klexReport the report
	 * @param parameters the parameter map
	 * @param conn the connection
	 * @return the handle
	 * @throws JRException
	 */
	public static AsynchronousFillHandle createHandle(
		KlexReportsContext klexReportsContext,
		KlexReport klexReport,
		Map<String,Object> parameters,
		Connection conn
		) throws JRException
	{
		return createHandle(klexReportsContext, SimpleKlexReportSource.from(klexReport), 
				parameters, conn);
	}

	
	public static AsynchronousFillHandle createHandle(
		KlexReportsContext klexReportsContext,
		KlexReportSource reportSource,
		Map<String,Object> parameters,
		Connection conn
		) throws JRException
	{
		return new AsynchronousFillHandle(klexReportsContext, reportSource, parameters, null, conn);
	}


	/**
	 * Creates an asychronous filling handle.
	 * 
	 * @param klexReportsContext the context
	 * @param klexReport the report
	 * @param parameters the parameter map
	 * @return the handle
	 * @throws JRException
	 */
	public static AsynchronousFillHandle createHandle(
		KlexReportsContext klexReportsContext,
		KlexReport klexReport,
		Map<String,Object> parameters
		) throws JRException
	{
		return createHandle(klexReportsContext, SimpleKlexReportSource.from(klexReport), parameters);
	}
	
	public static AsynchronousFillHandle createHandle(
		KlexReportsContext klexReportsContext,
		KlexReportSource reportSource,
		Map<String,Object> parameters
		) throws JRException
	{
		return new AsynchronousFillHandle(klexReportsContext, reportSource, parameters, null, null);
	}
	
	
	/**
	 * @see #createHandle(KlexReportsContext, KlexReport, Map, JRDataSource)
	 */
	public static AsynchronousFillHandle createHandle(
		KlexReport klexReport,
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		return createHandle(DefaultKlexReportsContext.getInstance(), klexReport, parameters, dataSource);
	}


	/**
	 * @see #createHandle(KlexReportsContext, KlexReport, Map, Connection)
	 */
	public static AsynchronousFillHandle createHandle(
		KlexReport klexReport,
		Map<String,Object> parameters,
		Connection conn
		) throws JRException
	{
		return createHandle(DefaultKlexReportsContext.getInstance(), klexReport, parameters, conn);
	}


	/**
	 * @see #createHandle(KlexReportsContext, KlexReport, Map)
	 */
	public static AsynchronousFillHandle createHandle(
		KlexReport klexReport,
		Map<String,Object> parameters
		) throws JRException
	{
		return createHandle(DefaultKlexReportsContext.getInstance(), klexReport, parameters);
	}
	
	
	/**
	 * Sets the priority of the filler thread.
	 * 
	 * @param priority the filler thread priority.
	 * @see Thread#setPriority(int)
	 */
	public void setPriority (int priority)
	{
		synchronized (lock)
		{
			this.priority = priority;
			if (fillThread != null)
			{
				fillThread.setPriority(priority);
			}
		}
	}
	
	
	/**
	 * Sets the name of the filler thread.
	 * 
	 * @param name the filler thread name.
	 * @see Thread#setName(java.lang.String)
	 */
	public void setThreadName (String name)
	{
		synchronized (lock)
		{
			this.threadName = name;
			if (fillThread != null)
			{
				fillThread.setName(name);
			}
		}
	}
}
