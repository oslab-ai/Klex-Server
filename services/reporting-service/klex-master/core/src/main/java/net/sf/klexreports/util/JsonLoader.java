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
package net.sf.klexreports.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRClassLoader;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JsonLoader
{
	private static final Class jacksonUtilClass;
	private static final Method jacksonUtilGetInstanceMethod;

	private final KlexReportsContext klexReportsContext;
	private final Object jacksonUtilObject;
	private Method loadObjectMethod;
	private Method loadListMethod;
	
	static
	{
		Class localJacksonUtilClass = null;
		Method localGetInstanceMethod = null;
		try
		{
			localJacksonUtilClass = JRClassLoader.loadClassForRealName("net.sf.klexreports.jackson.util.JacksonUtil"); //FIXME7 which class loading method to use?
			localGetInstanceMethod = localJacksonUtilClass.getDeclaredMethod("getInstance", KlexReportsContext.class);
		}
		catch (ClassNotFoundException | NoSuchMethodException e)
		{
			//FIXME7 maybe make this configurable
		}
		jacksonUtilClass = localJacksonUtilClass;
		jacksonUtilGetInstanceMethod = localGetInstanceMethod;
	}

	/**
	 *
	 */
	protected JsonLoader(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
		
		if (jacksonUtilGetInstanceMethod == null)
		{
			this.jacksonUtilObject = null; 
		}
		else
		{
			try
			{
				this.jacksonUtilObject =  jacksonUtilGetInstanceMethod.invoke(null, klexReportsContext);
			}
			catch (InvocationTargetException | IllegalAccessException e)
			{
				throw new JRRuntimeException(e);
			}
		}
	}
	
	
	/**
	 *
	 */
	public static JsonLoader getInstance(KlexReportsContext klexReportsContext)
	{
		return new JsonLoader(klexReportsContext);
	}
	
	
	/**
	 *
	 */
	public <T> T loadObject(String jsonData, Class<T> clazz)
	{
		T result = null;
		if (jacksonUtilObject != null && jsonData != null) 
		{
			try
			{
				if (loadObjectMethod == null)
				{
					loadObjectMethod = jacksonUtilClass.getMethod("loadObject", String.class, Class.class);
				}
				result = (T)loadObjectMethod.invoke(jacksonUtilObject, jsonData, clazz);
			}
			catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
			{
				throw new JRRuntimeException(e);
			}
		}
		return result;
	}


	
	
	/**
	 *
	 */
	public <T> T loadObject(String jsonData, String className)
	{
		T result = null;
		if (jacksonUtilObject != null && jsonData != null) 
		{
			Class<T> clazz = null;
			try
			{
				clazz = (Class<T>)JRClassLoader.loadClassForRealName(className);
			}
			catch (ClassNotFoundException  e)
			{
				// nothing to do or maybe make this configurable
			}
			
			if (clazz != null)
			{
				try
				{
					if (loadObjectMethod == null)
					{
						loadObjectMethod = jacksonUtilClass.getMethod("loadObject", String.class, Class.class);
					}
					result = (T)loadObjectMethod.invoke(jacksonUtilObject, jsonData, clazz);
				}
				catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
				{
					throw new JRRuntimeException(e);
				}
			}
		}
		return result;
	}


	/**
	 * 
	 */
	public <T> List<T> loadList(String jsonData, Class<T> clazz)
	{
		List<T> result = null;
		if (jacksonUtilObject != null && jsonData != null) 
		{
			try
			{
				if (loadListMethod == null)
				{
					loadListMethod = jacksonUtilClass.getMethod("loadList", String.class, Class.class);
				}
				result = (List<T>)loadListMethod.invoke(jacksonUtilObject, jsonData, clazz);
			}
			catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
			{
				throw new JRRuntimeException(e);
			}
		}
		return result;
	}
}
