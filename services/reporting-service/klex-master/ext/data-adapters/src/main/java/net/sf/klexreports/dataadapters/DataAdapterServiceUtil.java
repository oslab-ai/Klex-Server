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

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ParameterContributorContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class DataAdapterServiceUtil
{
	public static final String EXCEPTION_MESSAGE_KEY_SERVICE_FACTORY_NOT_REGISTERED = "data.adapter.service.factory.not.registered";
	
	private ParameterContributorContext paramContribContext;


	/**
	 *
	 */
	private DataAdapterServiceUtil(ParameterContributorContext paramContribContext)
	{
		this.paramContribContext = paramContribContext;
	}
	
	
	/**
	 *
	 */
	public static DataAdapterServiceUtil getInstance(ParameterContributorContext paramContribContext)
	{
		return new DataAdapterServiceUtil(paramContribContext);
	}
	
	
	/**
	 *
	 */
	public DataAdapterService getService(DataAdapter dataAdapter)
	{
		KlexReportsContext klexReportsContext = paramContribContext.getKlexReportsContext();
		
		List<DataAdapterContributorFactory> bundles = klexReportsContext.getExtensions(
				DataAdapterContributorFactory.class);
		for (Iterator<DataAdapterContributorFactory> it = bundles.iterator(); it.hasNext();)
		{
			DataAdapterContributorFactory factory = it.next();
			DataAdapterService service = factory.getDataAdapterService(paramContribContext, dataAdapter);
			if (service != null)
			{
				return service;
			}
		}

		throw 
			new JRRuntimeException(
				EXCEPTION_MESSAGE_KEY_SERVICE_FACTORY_NOT_REGISTERED,
				new Object[]{dataAdapter.getName()});
	}
}
