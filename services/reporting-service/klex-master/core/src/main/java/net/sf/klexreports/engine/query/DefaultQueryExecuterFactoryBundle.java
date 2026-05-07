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
package net.sf.klexreports.engine.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRSingletonCache;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class DefaultQueryExecuterFactoryBundle implements JRQueryExecuterFactoryBundle
{
	private static final JRSingletonCache<QueryExecuterFactory> cache = 
			new JRSingletonCache<>(QueryExecuterFactory.class);
	
	private static final DefaultQueryExecuterFactoryBundle INSTANCE = new DefaultQueryExecuterFactoryBundle();
	

	private KlexReportsContext klexReportsContext;
	

	private DefaultQueryExecuterFactoryBundle()
	{
		this(DefaultKlexReportsContext.getInstance());
	}
	
	private DefaultQueryExecuterFactoryBundle(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	/**
	 * 
	 */
	public static DefaultQueryExecuterFactoryBundle getInstance()
	{
		return INSTANCE;
	}
	
	/**
	 * 
	 */
	public static DefaultQueryExecuterFactoryBundle getInstance(KlexReportsContext klexReportsContext)
	{
		return new DefaultQueryExecuterFactoryBundle(klexReportsContext);
	}

	@Override
	public String[] getLanguages()
	{
		List<String> languages = new ArrayList<>();
		List<PropertySuffix> properties = JRPropertiesUtil.getInstance(klexReportsContext).getProperties(QueryExecuterFactory.QUERY_EXECUTER_FACTORY_PREFIX);
		for (Iterator<PropertySuffix> it = properties.iterator(); it.hasNext();)
		{
			PropertySuffix property = it.next();
			languages.add(property.getSuffix());
		}
		return languages.toArray(new String[languages.size()]);
	}

	@Override
	public QueryExecuterFactory getQueryExecuterFactory(String language) throws JRException
	{
		String factoryClassName = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(QueryExecuterFactory.QUERY_EXECUTER_FACTORY_PREFIX + language);
		if (factoryClassName == null)
		{
			return null;
		}
		
		return cache.getCachedInstance(factoryClassName);
	}
}
