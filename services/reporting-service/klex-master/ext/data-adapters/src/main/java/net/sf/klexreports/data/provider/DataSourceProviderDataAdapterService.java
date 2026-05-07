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
package net.sf.klexreports.data.provider;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import net.sf.klexreports.dataadapters.AbstractClasspathAwareDataAdapterService;
import net.sf.klexreports.engine.JRDataSourceProvider;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.ParameterContributorContext;
import net.sf.klexreports.engine.util.JRClassLoader;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class DataSourceProviderDataAdapterService extends AbstractClasspathAwareDataAdapterService 
{
	private JRDataSourceProvider provider = null;

	/**
	 * 
	 */
	public DataSourceProviderDataAdapterService(
		ParameterContributorContext paramContribContext,
		DataSourceProviderDataAdapter dsDataAdapter
		) 
	{
		super(paramContribContext, dsDataAdapter);
	}

	public DataSourceProviderDataAdapter getDataSourceProviderDataAdapter() {
		return (DataSourceProviderDataAdapter) getDataAdapter();
	}
	
	@Override
	protected ClassLoader getClassLoader(ClassLoader cloader) {
		Object obj = getKlexReportsContext().getValue(CURRENT_CLASS_LOADER);
		if (obj != null && obj instanceof ClassLoader)
			cloader = (ClassLoader) obj;
		URL[] localURLs = getPathClassloader();
		if (localURLs == null || localURLs.length == 0)
			return cloader;
		return new URLClassLoader(localURLs, cloader);
	}
	
	public JRDataSourceProvider getProvider() throws JRException
	{
		if (provider == null)
		{
			DataSourceProviderDataAdapter dsDataAdapter = getDataSourceProviderDataAdapter();
			if (dsDataAdapter != null) 
			{
				ClassLoader oldThreadClassLoader = Thread.currentThread().getContextClassLoader(); 
				
				try 
				{
//					ClassLoader cloader = oldThreadClassLoader;
//					Object obj = getKlexReportsContext().getValue(CURRENT_CLASS_LOADER);
//					if(obj != null && obj instanceof ClassLoader)
//						cloader = (ClassLoader)obj ; 
//					Thread.currentThread().setContextClassLoader(
//						new CompositeClassloader(getClassLoader(), cloader)
//						);
					Thread.currentThread().setContextClassLoader(getClassLoader(oldThreadClassLoader));

					Class<?> clazz = JRClassLoader.loadClassForRealName(dsDataAdapter.getProviderClass());
					provider = (JRDataSourceProvider) clazz.getDeclaredConstructor().newInstance();
					// FIXME: I don't have a report, why I need a report??!
				}
				catch (ClassNotFoundException | IllegalAccessException | InstantiationException 
					| NoSuchMethodException | InvocationTargetException e) 
				{
					throw new JRException(e);
				}
				finally
				{
					Thread.currentThread().setContextClassLoader(oldThreadClassLoader);
				}
			}
		}
		
		return provider;
	}

	@Override
	public void contributeParameters(Map<String, Object> parameters) throws JRException 
	{
		JRDataSourceProvider dsProvider = getProvider();
		if (dsProvider != null) 
		{
			KlexReport jr = (KlexReport) parameters.get(JRParameter.KLEX_REPORT);
			parameters.put(JRParameter.REPORT_DATA_SOURCE, dsProvider.create(jr));
		}
	}

}
