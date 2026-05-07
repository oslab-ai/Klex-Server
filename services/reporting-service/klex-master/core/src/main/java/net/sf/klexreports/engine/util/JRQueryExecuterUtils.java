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
package net.sf.klexreports.engine.util;

import java.util.Iterator;
import java.util.List;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.JRQueryExecuterFactoryBundle;
import net.sf.klexreports.engine.query.QueryExecuterFactory;

/**
 * Query executer utility class.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public final class JRQueryExecuterUtils
{
	public static final String EXCEPTION_MESSAGE_KEY_QUERY_EXECUTER_FACTORY_NOT_REGISTERED = "util.query.executer.factory.not.registered";
	
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private JRQueryExecuterUtils(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	public static JRQueryExecuterUtils getInstance(KlexReportsContext klexReportsContext)
	{
		return new JRQueryExecuterUtils(klexReportsContext);
	}
	
	
	/**
	 * Returns a query executer factory for a query language.
	 * 
	 * @param language the query language
	 * @return a query executer factory
	 * @throws JRException
	 * @see QueryExecuterFactory#QUERY_EXECUTER_FACTORY_PREFIX
	 */
	public QueryExecuterFactory getExecuterFactory(String language) throws JRException
	{
		List<JRQueryExecuterFactoryBundle> bundles = klexReportsContext.getExtensions(
				JRQueryExecuterFactoryBundle.class);
		for (Iterator<JRQueryExecuterFactoryBundle> it = bundles.iterator(); it.hasNext();)
		{
			JRQueryExecuterFactoryBundle bundle = it.next();
			QueryExecuterFactory factory = bundle.getQueryExecuterFactory(language);
			if (factory != null)
			{
				return factory;
			}
		}
		throw 
			new JRRuntimeException(
				EXCEPTION_MESSAGE_KEY_QUERY_EXECUTER_FACTORY_NOT_REGISTERED,
				new Object[]{language});
	}
}

