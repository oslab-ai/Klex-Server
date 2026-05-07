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
package net.sf.klexreports.dataadapters;

import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ParameterContributorContext;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class AbstractDataAdapterService implements DataAdapterService
{
	public static final String SECRETS_CATEGORY = "net.sf.klexreports.data.adapter";

	private final ParameterContributorContext paramContribContext;
	private String name;
	private DataAdapter dataAdapter;

 	/**
	 * @deprecated Replaced by {@link #AbstractDataAdapterService(ParameterContributorContext, DataAdapter)}.
	 */
	protected AbstractDataAdapterService(KlexReportsContext klexReportsContext, DataAdapter dataAdapter)
	{
		this(new ParameterContributorContext(klexReportsContext, null, null), dataAdapter);
	}
	  
	/**
	 *
	 */
	public AbstractDataAdapterService(ParameterContributorContext paramContribContext, DataAdapter dataAdapter)
	{
		this.dataAdapter = dataAdapter;
		this.paramContribContext = paramContribContext;
	}
	  
	/**
	 *
	 */
	public ParameterContributorContext getParameterContributorContext()
	{
		return paramContribContext;
	}
	  
	/**
	 *
	 */
	public KlexReportsContext getKlexReportsContext()
	{
		return paramContribContext == null ? null : paramContribContext.getKlexReportsContext();
	}
	  
	/**
	 *
	 */
	public String getName()
	{
		return name;
	}
	  
	/**
	 *
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	  
	/**
	 *
	 */
	public DataAdapter getDataAdapter()
	{
		return dataAdapter;
	}
	  
	/**
	 * FIXME consider removing
	 */
	public void setDataAdapter(DataAdapter dataAdapter)
	{
		this.dataAdapter = dataAdapter;
	}
	  
	@Override
	public abstract void contributeParameters(Map<String, Object> parameters) throws JRException;
	
	@Override
	public void dispose() 
	{
	}

	@Override
	public void test() throws JRException
	{
		contributeParameters(new HashMap<>());
		dispose();
	}
 
}
