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

import java.util.Iterator;
import java.util.List;

import net.sf.klexreports.data.bean.BeanDataAdapter;
import net.sf.klexreports.data.bean.BeanDataAdapterService;
import net.sf.klexreports.data.csv.CsvDataAdapter;
import net.sf.klexreports.data.csv.CsvDataAdapterService;
import net.sf.klexreports.data.ds.DataSourceDataAdapter;
import net.sf.klexreports.data.ds.DataSourceDataAdapterService;
import net.sf.klexreports.data.empty.EmptyDataAdapter;
import net.sf.klexreports.data.empty.EmptyDataAdapterService;
import net.sf.klexreports.data.jdbc.JdbcDataAdapter;
import net.sf.klexreports.data.jdbc.JdbcDataAdapterContributorFactory;
import net.sf.klexreports.data.jdbc.JdbcDataAdapterService;
import net.sf.klexreports.data.jndi.JndiDataAdapter;
import net.sf.klexreports.data.jndi.JndiDataAdapterService;
import net.sf.klexreports.data.json.JsonDataAdapter;
import net.sf.klexreports.data.json.JsonDataAdapterService;
import net.sf.klexreports.data.provider.DataSourceProviderDataAdapter;
import net.sf.klexreports.data.provider.DataSourceProviderDataAdapterService;
import net.sf.klexreports.data.qe.QueryExecuterDataAdapter;
import net.sf.klexreports.data.qe.QueryExecuterDataAdapterService;
import net.sf.klexreports.data.random.RandomDataAdapter;
import net.sf.klexreports.data.random.RandomDataAdapterService;
import net.sf.klexreports.data.xml.XmlDataAdapter;
import net.sf.klexreports.data.xml.XmlDataAdapterService;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ParameterContributorContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class DefaultDataAdapterServiceFactory implements DataAdapterContributorFactory
{

	/**
	 *
	 */
	private static final DefaultDataAdapterServiceFactory INSTANCE = new DefaultDataAdapterServiceFactory();

	/**
	 *
	 */
	private DefaultDataAdapterServiceFactory()
	{
	}

	/**
	 *
	 */
	public static DefaultDataAdapterServiceFactory getInstance()
	{
		return INSTANCE;
	}
	
	@Override
	public DataAdapterService getDataAdapterService(ParameterContributorContext context, DataAdapter dataAdapter)
	{
		//KlexReportsContext klexReportsContext = context.getKlexReportsContext();
		DataAdapterService dataAdapterService = null;
		
		if (dataAdapter instanceof BeanDataAdapter)
		{
			dataAdapterService = new BeanDataAdapterService(context, (BeanDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof CsvDataAdapter)
		{
			dataAdapterService = new CsvDataAdapterService(context, (CsvDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof DataSourceDataAdapter)
		{
			dataAdapterService = new DataSourceDataAdapterService(context, (DataSourceDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof EmptyDataAdapter)
		{
			dataAdapterService = new EmptyDataAdapterService(context, (EmptyDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof RandomDataAdapter)
		{
			dataAdapterService = new RandomDataAdapterService(context, (RandomDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof JndiDataAdapter)
		{
			dataAdapterService = new JndiDataAdapterService(context, (JndiDataAdapter)dataAdapter);//FIXME maybe want some cache here
		}
		else if (dataAdapter instanceof DataSourceProviderDataAdapter)
		{
			dataAdapterService = new DataSourceProviderDataAdapterService(context, (DataSourceProviderDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof QueryExecuterDataAdapter)
		{
			dataAdapterService = new QueryExecuterDataAdapterService(context, (QueryExecuterDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof XmlDataAdapter)
		{
			dataAdapterService = new XmlDataAdapterService(context, (XmlDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof JsonDataAdapter)
		{
			dataAdapterService = new JsonDataAdapterService(context, (JsonDataAdapter)dataAdapter);
		}
		else if (dataAdapter instanceof JdbcDataAdapter)
		{
			KlexReportsContext klexReportsContext = context.getKlexReportsContext();
			
			List<JdbcDataAdapterContributorFactory> bundles = klexReportsContext.getExtensions(
					JdbcDataAdapterContributorFactory.class);
			for (Iterator<JdbcDataAdapterContributorFactory> it = bundles.iterator(); it.hasNext();)
			{
				JdbcDataAdapterContributorFactory factory = it.next();
				DataAdapterService service = factory.getDataAdapterService(context, (JdbcDataAdapter)dataAdapter);
				if (service != null)
				{
					dataAdapterService = service;
					break;
				}
			}

			if (dataAdapterService == null)
			{
				dataAdapterService = new JdbcDataAdapterService(context, (JdbcDataAdapter)dataAdapter);
			}
		}
		
		return dataAdapterService;
	}
  
}
