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
package net.sf.klexreports.export;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.ClassUtils;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class CompositeExporterConfigurationFactory<C extends CommonExportConfiguration>
{
	/**
	 * 
	 */
	private final JRPropertiesUtil propertiesUtil;
	private final Class<C> configurationInterface;
	
	/**
	 * 
	 */
	public CompositeExporterConfigurationFactory(KlexReportsContext klexReportsContext, Class<C> configurationInterface)
	{
		this.propertiesUtil = JRPropertiesUtil.getInstance(klexReportsContext);
		this.configurationInterface = configurationInterface;
	}

	
	/**
	 * 
	 */
	public C getConfiguration(final C parent, final C child)
	{
		if (parent == null)
		{
			return child;
		}
		else
		{
			boolean isOverrideHints = 
				parent.isOverrideHints() == null 
				? propertiesUtil.getBooleanProperty(ExporterConfiguration.PROPERTY_EXPORT_CONFIGURATION_OVERRIDE_REPORT_HINTS)
				: parent.isOverrideHints();
			return getConfiguration(parent, child, isOverrideHints);
		}
	}

	
	/**
	 * 
	 */
	public C getConfiguration(final C parent, final C child, boolean isOverrideHints)
	{
		if (parent == null)
		{
			return child;
		}
		else
		{
			if (isOverrideHints)
			{
				return getProxy(configurationInterface, new DelegateInvocationHandler(child, parent));
			}
			else
			{
				return getProxy(configurationInterface, new DelegateInvocationHandler(parent, child));
			}
		}
	}


	/**
	 * 
	 */
	private final C getProxy(Class<?> clazz, InvocationHandler handler)
	{
		List<Class<?>> allInterfaces = new ArrayList<>();

		if (clazz.isInterface())
		{
			allInterfaces.add(clazz);
		}
		else
		{
			List<Class<?>> lcInterfaces = ClassUtils.getInterfaces(clazz);
			allInterfaces.addAll(lcInterfaces);
		}

		@SuppressWarnings("unchecked")
		C composite =
			(C)Proxy.newProxyInstance(
				ExporterConfiguration.class.getClassLoader(),
				allInterfaces.toArray(new Class<?>[allInterfaces.size()]),
				handler
				);
		
		return composite;
	}


	/**
	 * 
	 */
	class DelegateInvocationHandler implements InvocationHandler
	{
		private final C parent;
		private final C child;
		
		/**
		 * 
		 */
		public DelegateInvocationHandler(final C parent, final C child)
		{
			this.parent = parent;
			this.child = child;
		}
		
		@Override
		public Object invoke(
			Object proxy, 
			Method method, 
			Object[] args
			) throws Throwable 
		{
			Object value = child == null ? null : method.invoke(child, args);
			if (value == null)
			{
				value = parent == null ? null : method.invoke(parent, args);
			}
			return value;
		}
	}
}
