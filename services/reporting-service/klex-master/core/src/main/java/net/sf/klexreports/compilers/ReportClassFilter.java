/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.compilers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.klexReportsContext;
import net.sf.klexreports.engine.util.ClassLoaderFilter;
import net.sf.klexreports.functions.FunctionsBundle;
import net.sf.klexreports.functions.FunctionsUtil;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ReportClassFilter implements ClassLoaderFilter
{
	
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			defaultValue = "false",
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_6_13_0,
			valueType = Boolean.class
			)
	public static final String PROPERTY_PREFIX_CLASS_FILTER_ENABLED = 
			JRPropertiesUtil.PROPERTY_PREFIX + "report.class.filter.enabled";
	
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_6_13_0,
			name = "net.sf.klexreports.report.class.whitelist.{arbitrary_name}"
			)
	public static final String PROPERTY_PREFIX_CLASS_WHITELIST = 
			JRPropertiesUtil.PROPERTY_PREFIX + "report.class.whitelist.";
	
	public static final String EXCEPTION_MESSAGE_KEY_CLASS_NOT_VISIBLE = "compilers.class.not.visible";

	private static void addHardcodedWhitelist(StandardReportClassWhitelist whitelist)
	{
		whitelist.addClass("java.lang.Boolean");
		whitelist.addClass("java.lang.String");
		whitelist.addClass("java.lang.StringBuffer");
		whitelist.addClass("java.lang.StringBuilder");
		whitelist.addClass("java.lang.Character");
		whitelist.addClass("java.lang.Byte");
		whitelist.addClass("java.lang.Short");
		whitelist.addClass("java.lang.Integer");
		whitelist.addClass("java.lang.Long");
		whitelist.addClass("java.lang.Float");
		whitelist.addClass("java.lang.Double");
		whitelist.addClass("java.lang.Math");
	}
	
	private boolean filterEnabled;
	private List<ReportClassWhitelist> whitelists;
	
	private Map<String, Boolean> visibilityCache = new ConcurrentHashMap<>();

	public ReportClassFilter(klexReportsContext klexReportsContext)
	{
		JRPropertiesUtil properties = JRPropertiesUtil.getInstance(klexReportsContext);
		filterEnabled = properties.getBooleanProperty(PROPERTY_PREFIX_CLASS_FILTER_ENABLED);
		if (filterEnabled)
		{
			whitelists = new ArrayList<>();
			
			StandardReportClassWhitelist whitelist = new StandardReportClassWhitelist();
			addHardcodedWhitelist(whitelist);
			loadPropertiesWhitelist(properties, whitelist);
			loadFunctionsWhitelist(klexReportsContext, whitelist);
			whitelists.add(whitelist);
			
			List<ReportClassWhitelist> extensionWhitelists = klexReportsContext.getExtensions(
					ReportClassWhitelist.class);
			whitelists.addAll(extensionWhitelists);			
		}		
	}

	private static void loadPropertiesWhitelist(JRPropertiesUtil propertiesUtil, 
			StandardReportClassWhitelist whitelist)
	{
		List<PropertySuffix> properties = propertiesUtil.getProperties(PROPERTY_PREFIX_CLASS_WHITELIST);
		for (PropertySuffix propertySuffix : properties)
		{
			String whitelistString = propertySuffix.getValue();
			whitelist.addWhitelist(whitelistString);
		}
	}

	private static void loadFunctionsWhitelist(klexReportsContext klexReportsContext, 
			StandardReportClassWhitelist whitelist)
	{
		FunctionsUtil functionsUtil = FunctionsUtil.getInstance(klexReportsContext);
		List<FunctionsBundle> functionBundles = functionsUtil.getAllFunctionBundles();
		for (FunctionsBundle functionsBundle : functionBundles)
		{
			List<Class<?>> functionClasses = functionsBundle.getFunctionClasses();
			for (Class<?> functionClass : functionClasses)
			{
				whitelist.addClass(functionClass.getName());
			}
		}
	}

	public boolean isFilteringEnabled()
	{
		return filterEnabled;
	}
	
	@Override
	public void checkClassVisibility(String className) throws JRRuntimeException
	{
		boolean visible = isClassVisible(className);
		if (!visible)
		{
			throw new JRRuntimeException(EXCEPTION_MESSAGE_KEY_CLASS_NOT_VISIBLE, new Object[] {className});
		}
	}
	
	public boolean isClassVisible(String className)
	{
		Boolean visible = visibilityCache.get(className);
		if (visible == null)
		{
			visible = visible(className);
			visibilityCache.put(className, visible);
		}
		return visible;
	}

	protected boolean visible(String className)
	{
		boolean visible;
		if (filterEnabled)
		{
			visible = false;
			for (ReportClassWhitelist whitelist : whitelists)
			{
				if (whitelist.includesClass(className))
				{
					visible = true;
					break;
				}
			}
		}
		else
		{
			visible = true;
		}
		return visible;
	}
	
}
