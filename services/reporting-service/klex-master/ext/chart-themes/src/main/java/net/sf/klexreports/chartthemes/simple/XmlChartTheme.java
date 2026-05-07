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
package net.sf.klexreports.chartthemes.simple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.engine.util.VersionComparator;
import net.sf.klexreports.engine.xml.JRXmlWriter;
import net.sf.klexreports.jackson.util.JacksonUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class XmlChartTheme extends SimpleChartTheme
{
	/**
	 *
	 */
	private String file;
	
	/**
	 *
	 */
	public XmlChartTheme()
	{
	}
	
	/**
	 *
	 */
	public XmlChartTheme(String file)
	{
		this.file = file;
	}
	

	/**
	 *
	 */
	public void setFile(String file)
	{
		this.file = file;
		this.chartThemeSettings = null;
	}
	
	
	@Override
	public ChartThemeSettings getChartThemeSettings()
	{
		if (chartThemeSettings == null)
		{
			chartThemeSettings = loadSettings(file);
		}
		return chartThemeSettings;
	}
	
	
	/**
	 *
	 */
	public static ChartThemeSettings loadSettings(String file)
	{
		InputStream is = null; 
		
		try
		{
			is = JRLoader.getLocationInputStream(file);
			return loadSettings(is);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
		finally
		{
			if (is != null)
			{
				try
				{
					is.close();
				}
				catch(IOException e)
				{
				}
			}
		}
	}
	
	
	/**
	 *
	 */
	public static ChartThemeSettings loadSettings(InputStream is)
	{
		return JacksonUtil.getInstance(DefaultKlexReportsContext.getInstance()).loadXml(is, ChartThemeSettings.class);
	}
	
	
	/**
	 * @deprecated Replaced by {@link #saveSettings(KlexReportsContext, ChartThemeSettings, Writer)}.
	 */
	public static void saveSettings(ChartThemeSettings settings, Writer writer)
	{
		saveSettings(DefaultKlexReportsContext.getInstance(), settings, writer);
	}
	

	/**
	 *
	 */
	public static void saveSettings(KlexReportsContext klexReportsContext, ChartThemeSettings settings, Writer writer)
	{
		String targetVersion = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(JRXmlWriter.PROPERTY_REPORT_VERSION);
		VersionComparator versionComparator = new VersionComparator();
		
		if (versionComparator.compare(targetVersion, JRConstants.VERSION_6_19_0) >= 0)
		{
			String xml = JacksonUtil.getInstance(DefaultKlexReportsContext.getInstance()).getXmlString(settings);
			try
			{
				writer.write(xml);
			}
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
		}
		else
		{
			try
			{
				Class clazz = XmlChartTheme.class.getClassLoader().loadClass("net.sf.klexreports.chartthemes.simple.XmlChartThemeCastorWriter");
				Method method = clazz.getMethod("saveSettings", ChartThemeSettings.class, Writer.class);
				method.invoke(null, settings, writer);
			}
			catch (ClassNotFoundException | NoSuchMethodException |  InvocationTargetException |  IllegalAccessException e)
			{
				throw new JRRuntimeException(e);
			}
		}
	}
	

	/**
	 * @deprecated Replaced by {@link #saveSettings(KlexReportsContext, ChartThemeSettings, File)}.
	 */
	public static void saveSettings(ChartThemeSettings settings, File file)
	{
		saveSettings(DefaultKlexReportsContext.getInstance(), settings, file);
	}
	

	/**
	 *
	 */
	public static void saveSettings(KlexReportsContext klexReportsContext, ChartThemeSettings settings, File file)
	{
		Writer writer = null;
		
		try
		{
			writer = new FileWriter(file);
			saveSettings(klexReportsContext, settings, writer);
		}
		catch (IOException e)
		{
			throw new JRRuntimeException(e);
		}
		finally
		{
			if (writer != null)
			{
				try
				{
					writer.close();
				}
				catch(IOException e)
				{
				}
			}
		}
	}
	

	/**
	 * @deprecated Replaced by {@link #saveSettings(KlexReportsContext, ChartThemeSettings)}.
	 */
	public static String saveSettings(ChartThemeSettings settings)
	{
		return saveSettings(DefaultKlexReportsContext.getInstance(), settings);
	}
	

	/**
	 *
	 */
	public static String saveSettings(KlexReportsContext klexReportsContext, ChartThemeSettings settings)
	{
		StringWriter writer = new StringWriter();
		
		try
		{
			saveSettings(klexReportsContext, settings, writer);
		}
		finally
		{
			try
			{
				writer.close();
			}
			catch(IOException e)
			{
			}
		}
		
		return writer.toString();
	}

}
