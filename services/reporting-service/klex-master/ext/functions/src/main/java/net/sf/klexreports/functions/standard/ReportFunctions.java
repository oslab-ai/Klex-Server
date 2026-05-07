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
package net.sf.klexreports.functions.standard;

import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.data.HierarchicalDataSource;
import net.sf.klexreports.engine.fill.SortedDataSource;
import net.sf.klexreports.functions.AbstractFunctionSupport;
import net.sf.klexreports.functions.annotations.Function;
import net.sf.klexreports.functions.annotations.FunctionCategories;
import net.sf.klexreports.functions.annotations.FunctionParameter;
import net.sf.klexreports.functions.annotations.FunctionParameters;
import net.sf.klexreports.repo.RepositoryContext;
import net.sf.klexreports.repo.RepositoryUtil;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
@FunctionCategories({ReportCategory.class})
public class ReportFunctions extends AbstractFunctionSupport
{

	public static final String EXCEPTION_DATA_SOURCE_NOT_HIERARCHICAL = "data.source.not.hierarchical";
	
	@Function("ORIGINAL_DATA_SOURCE")
	public JRDataSource ORIGINAL_DATA_SOURCE()
	{
		try
		{
			return originalDataSource();
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	protected JRDataSource originalDataSource() throws JRException
	{
		JRDataSource dataSource = (JRDataSource) getContext().getParameterValue(JRParameter.REPORT_DATA_SOURCE);
		if (dataSource instanceof SortedDataSource)
		{
			dataSource = ((SortedDataSource) dataSource).getOriginalDataSource();
		}
		return dataSource;
	}
	
	@Function("SUB_DATA_SOURCE")
	public JRDataSource SUB_DATA_SOURCE()
	{
		try
		{
			HierarchicalDataSource<?> hierarchicalDataSource = hierarchicalDataSource();
			return hierarchicalDataSource.subDataSource();
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}
	
	@Function("SUB_DATA_SOURCE")
	@FunctionParameters({@FunctionParameter("expression")})
	public JRDataSource SUB_DATA_SOURCE(String expression)
	{
		try
		{
			HierarchicalDataSource<?> hierarchicalDataSource = hierarchicalDataSource();
			return hierarchicalDataSource.subDataSource(expression);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	protected HierarchicalDataSource<?> hierarchicalDataSource() throws JRException
	{
		JRDataSource dataSource = originalDataSource();
		if (dataSource == null)
		{
			//usually caught in JREvaluator
			throw new NullPointerException();
		}
		if (!(dataSource instanceof HierarchicalDataSource<?>))
		{
			throw new JRRuntimeException(EXCEPTION_DATA_SOURCE_NOT_HIERARCHICAL, 
					new Object[] {dataSource.getClass().getName()}); 
		}
		HierarchicalDataSource<?> hierarchicalDataSource = (HierarchicalDataSource<?>) dataSource;
		return hierarchicalDataSource;
	}
	
	@Function("RESOURCE_DATA")
	@FunctionParameters({@FunctionParameter("location")})
	public byte[] RESOURCE_DATA(String location)
	{
		RepositoryUtil repository;
		Object repositoryContext = getContext().getParameterValue(JRParameter.REPOSITORY_CONTEXT, true);
		if (repositoryContext instanceof RepositoryContext)
		{
			repository = RepositoryUtil.getInstance((RepositoryContext) repositoryContext);
		}
		else
		{
			KlexReportsContext klexReportsContext = (KlexReportsContext) getContext().getParameterValue(
					JRParameter.KLEX_REPORTS_CONTEXT);
			repository = RepositoryUtil.getInstance(klexReportsContext);
		}

		try
		{
			return repository.getBytesFromLocation(location);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}
}
