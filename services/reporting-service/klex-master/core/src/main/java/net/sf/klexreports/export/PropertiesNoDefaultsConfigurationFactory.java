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

import java.awt.Color;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.klexreports.engine.JRPropertiesHolder;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.type.NamedEnum;
import net.sf.klexreports.engine.util.ClassUtils;
import net.sf.klexreports.engine.util.JRColorUtil;
import net.sf.klexreports.export.annotations.ExporterProperty;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PropertiesNoDefaultsConfigurationFactory<C extends CommonExportConfiguration>
{
	/**
	 * 
	 */
	private final KlexReportsContext klexReportsContext;
	
	/**
	 * 
	 */
	public PropertiesNoDefaultsConfigurationFactory(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}

	
	/**
	 * 
	 */
	public C getConfiguration(final Class<C> configurationInterface, final JRPropertiesHolder propertiesHolder)
	{
		return getProxy(configurationInterface, new PropertiesInvocationHandler(propertiesHolder));
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
		C proxy =
			(C)Proxy.newProxyInstance(
				ExporterConfiguration.class.getClassLoader(),
				allInterfaces.toArray(new Class<?>[allInterfaces.size()]),
				handler
				);
		
		return proxy;
	}


	/**
	 * 
	 */
	class PropertiesInvocationHandler implements InvocationHandler
	{
		private final JRPropertiesHolder propertiesHolder;
		
		/**
		 * 
		 */
		public PropertiesInvocationHandler(final JRPropertiesHolder propertiesHolder)
		{
			this.propertiesHolder = propertiesHolder;
		}
		
		@Override
		public Object invoke(
			Object proxy, 
			Method method, 
			Object[] args
			) throws Throwable 
		{
			return getPropertyValue(method, propertiesHolder);
		}
	}
	
	
	/**
	 * 
	 */
	protected Object getPropertyValue(Method method, JRPropertiesHolder propertiesHolder)
	{
		Object value = null;
		ExporterProperty exporterProperty = method.getAnnotation(ExporterProperty.class);
		if (exporterProperty != null)
		{
			value = getPropertyValue(klexReportsContext, propertiesHolder, exporterProperty, method.getReturnType());
		}
		return value;
	}
	
	
	/**
	 * 
	 */
	public static Object getPropertyValue(
		KlexReportsContext klexReportsContext,
		JRPropertiesHolder propertiesHolder,
		ExporterProperty exporterProperty, 
		Class<?> type 
		)
	{
		Object value = null;
		
		String propertyName = exporterProperty.value();
		
		if (String[].class.equals(type))
		{
			List<PropertySuffix> properties = JRPropertiesUtil.getProperties(propertiesHolder, propertyName);
			if (properties != null && !properties.isEmpty())
			{
				String[] values = new String[properties.size()];
				for(int i = 0; i < values.length; i++)
				{
					values[i] = properties.get(i).getValue();
				}
				
				value = values;
			}
		}
		else if (PropertySuffix[].class.equals(type))
		{
			List<PropertySuffix> properties = JRPropertiesUtil.getProperties(propertiesHolder, propertyName);
			if (properties != null && !properties.isEmpty())
			{
				value = properties.toArray(new PropertySuffix[properties.size()]);
			}
		}
		else if (Map.class.equals(type))
		{
			List<PropertySuffix> properties = JRPropertiesUtil.getProperties(propertiesHolder, propertyName);
			if (properties != null && !properties.isEmpty())
			{
				Map<String,String> values = new HashMap<>();
				for (PropertySuffix propertySuffix : properties)
				{
					values.put(propertySuffix.getSuffix(), propertySuffix.getValue());
				}
				value = values;
			}
		}
		else
		{
			String strValue = null;

			JRPropertiesMap propertiesMap = propertiesHolder.getPropertiesMap();
			if (propertiesMap != null && propertiesMap.containsProperty(propertyName))
			{
				strValue = propertiesMap.getProperty(propertyName);
			}

			if (strValue != null)
			{
				if (String.class.equals(type))
				{
					value = strValue;
				}
				else if (Character.class.equals(type))
				{
					value = JRPropertiesUtil.asCharacter(strValue);
				}
				else if (Integer.class.equals(type))
				{
					value = JRPropertiesUtil.asInteger(strValue);
				}
				else if (Long.class.equals(type))
				{
					value = JRPropertiesUtil.asLong(strValue);
				}
				else if (Float.class.equals(type))
				{
					value = JRPropertiesUtil.asFloat(strValue);
				}
				else if (Boolean.class.equals(type))
				{
					value = JRPropertiesUtil.asBoolean(strValue);
				}
				else if (Color.class.equals(type))
				{
					value = strValue == null ? null : JRColorUtil.getColor(strValue, null);
				}
				else if (NamedEnum.class.isAssignableFrom(type))
				{
					try
					{
						Method byNameMethod = type.getMethod("getByName", new Class<?>[]{String.class});
						value = byNameMethod.invoke(null, strValue);
					}
					catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
					{
						throw new JRRuntimeException(e);
					}
				}
				else
				{
					throw 
					new JRRuntimeException(
						PropertiesExporterConfigurationFactory.EXCEPTION_MESSAGE_KEY_EXPORT_PROPERTIES_TYPE_NOT_SUPPORTED, 
						new Object[]{type});
				}
			}
		}
		
		return value;
	}
}
