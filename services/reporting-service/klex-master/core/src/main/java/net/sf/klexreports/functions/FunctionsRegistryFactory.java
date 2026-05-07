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
package net.sf.klexreports.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.extensions.DefaultExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.SingletonExtensionRegistry;

/**
 * @author Massimo Rabbi (mrabbi@users.sourceforge.net)
 */
public class FunctionsRegistryFactory  implements ExtensionsRegistryFactory
{
	/**
	 * The key used inside a klexreports_extensions.properties file to denote the classes
	 * referenced by this extension 
	 */
	public final static String FUNCTIONS_CLASSES_PROPERTY_PREFIX = 
		DefaultExtensionsRegistry.PROPERTY_REGISTRY_PREFIX + "functions.";
	
	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties)
	{
		List<String> classNames = new ArrayList<>();

		addFunctionClasses(classNames, properties, FUNCTIONS_CLASSES_PROPERTY_PREFIX);
		
		return new SingletonExtensionRegistry<FunctionsBundle>(FunctionsBundle.class, new FunctionsBundle(classNames));
	}
	
	/**
	 * 
	 */
	private void addFunctionClasses(List<String> classNames, JRPropertiesMap properties, String propertyPrefix)
	{
		List<PropertySuffix> functionClassProperties = JRPropertiesUtil.getProperties(properties, propertyPrefix);
		for (Iterator<PropertySuffix> it = functionClassProperties.iterator(); it.hasNext();)
		{
			PropertySuffix functionsClassesProp = it.next(); 

			// We assume this property value is a comma-separated class names list like: a.b.c.ClassA, a.b.d.ClassB
			
			String[] classes = functionsClassesProp.getValue().split(",");
			
			for (String className : classes)
			{
				className = className.trim();
				if (className.length() > 0)
				{
					classNames.add( className);
				}
			}
		}
	}
	
}

