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
package net.sf.klexreports.data.bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import net.sf.klexreports.dataadapters.AbstractClasspathAwareDataAdapterService;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.ParameterContributorContext;
import net.sf.klexreports.engine.data.JRAbstractBeanDataSource;
import net.sf.klexreports.engine.data.JRBeanArrayDataSource;
import net.sf.klexreports.engine.data.JRBeanCollectionDataSource;
import net.sf.klexreports.engine.util.JRClassLoader;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class BeanDataAdapterService extends AbstractClasspathAwareDataAdapterService 
{

	public static final String EXCEPTION_MESSAGE_KEY_INVALID_RETURN_TYPE = "data.bean.invalid.return.type";
	
	/**
	 * 
	 */
	public BeanDataAdapterService(ParameterContributorContext paramContribContext, BeanDataAdapter beanDataAdapter) 
	{
		super(paramContribContext, beanDataAdapter);
	}

	public BeanDataAdapter getBeanDataAdapter() {
		return (BeanDataAdapter) getDataAdapter();
	}

	@Override
	public void contributeParameters(Map<String, Object> parameters) throws JRException 
	{
		BeanDataAdapter beanDataAdapter = getBeanDataAdapter();
		if (beanDataAdapter != null)
		{
			JRAbstractBeanDataSource beanDataSource = null;

			ClassLoader oldThreadClassLoader = Thread.currentThread().getContextClassLoader();

			try 
			{
				Thread.currentThread().setContextClassLoader(getClassLoader(oldThreadClassLoader));

				Class<?> clazz = JRClassLoader.loadClassForRealName(beanDataAdapter.getFactoryClass());
				Method method = clazz.getMethod(beanDataAdapter.getMethodName());
				Object res = method.invoke(null);
				if (res instanceof Collection) {
					beanDataSource = new JRBeanCollectionDataSource(
							(Collection<?>) res,
							beanDataAdapter.isUseFieldDescription());
				} else if (res instanceof Object[]) {
					beanDataSource = new JRBeanArrayDataSource((Object[]) res,
							beanDataAdapter.isUseFieldDescription());
				} else {
					throw 
						new JRException(
							EXCEPTION_MESSAGE_KEY_INVALID_RETURN_TYPE,
							new Object[]{clazz.getName()});
				}
			}
			catch (ClassNotFoundException | IllegalAccessException | SecurityException 
				| NoSuchMethodException | IllegalArgumentException | InvocationTargetException e) 
			{
				throw new JRException(e);
			}
			finally
			{
				Thread.currentThread().setContextClassLoader(oldThreadClassLoader);
			}

			parameters.put(JRParameter.REPORT_DATA_SOURCE, beanDataSource);
		}
	}
}
