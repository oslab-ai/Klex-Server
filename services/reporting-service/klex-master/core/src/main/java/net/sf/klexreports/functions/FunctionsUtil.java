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

import java.lang.reflect.Method;
import java.util.List;

import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class FunctionsUtil
{

	/**
	 * 
	 */
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private FunctionsUtil(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	public static FunctionsUtil getInstance(KlexReportsContext klexReportsContext)
	{
		return new FunctionsUtil(klexReportsContext);
	}
	
	/**
	 * 
	 */
	public List<FunctionsBundle> getAllFunctionBundles()
	{
		List<FunctionsBundle> bundles = klexReportsContext.getExtensions(FunctionsBundle.class);
		return bundles;
	}
	
	/*
	 * 
	 *
	public Class<?> getClass4Function(String functionName)
	{
		List<FunctionsBundle> bundles = klexReportsContext.getExtensions(FunctionsBundle.class);
		for (FunctionsBundle bundle : bundles)
		{
			List<Class<?>> classes = bundle.getFunctionClasses();
			for (Class<?> clazz : classes)
			{
				Method[] methods = clazz.getMethods();
				for (Method method : methods)
				{
					if (functionName.equals(method.getName()))
					{
						return clazz;
					}
				}
			}
		}
		
		return null;
	}
	*/

	/**
	 * 
	 */
	public Method getMethod4Function(String functionName)
	{
		List<FunctionsBundle> bundles = klexReportsContext.getExtensions(FunctionsBundle.class);
		for (FunctionsBundle bundle : bundles)
		{
			List<Class<?>> classes = bundle.getFunctionClasses();
			for (Class<?> clazz : classes)
			{
				Method[] methods = clazz.getMethods();
				for (Method method : methods)
				{
					if (functionName.equals(method.getName()))
					{
						return method;
					}
				}
			}
		}
		
		return null;
	}

}
