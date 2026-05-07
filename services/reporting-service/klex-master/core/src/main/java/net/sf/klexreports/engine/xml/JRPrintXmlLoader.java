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
package net.sf.klexreports.engine.xml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.xml.print.PrintXmlLoader;


/**
 * Utility class that helps reconverting XML documents into 
 * {@link net.sf.klexreports.engine.KlexPrint} objects. 
 * <p>
 * Generated documents can be stored in XML format if they are exported using the
 * {@link net.sf.klexreports.engine.export.JRXmlExporter}. After they're exported,
 * one can parse them back into {@link net.sf.klexreports.engine.KlexPrint} objects
 * by using this class.
 * </p>
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRPrintXmlLoader
{
	
	private static final Log log = LogFactory.getLog(JRPrintXmlLoader.class);
	
	/**
	 *
	 */
	private final KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	protected JRPrintXmlLoader(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}


	/**
	 *
	 */
	public KlexReportsContext getKlexReportsContext()
	{
		return klexReportsContext;
	}


	/**
	 *
	 */
	public static KlexPrint loadFromFile(KlexReportsContext klexReportsContext, String sourceFileName) throws JRException
	{
		KlexPrint klexPrint = null;

		try (FileInputStream fis = new FileInputStream(sourceFileName))
		{
			JRPrintXmlLoader printXmlLoader = new JRPrintXmlLoader(klexReportsContext);
			klexPrint = printXmlLoader.loadXML(fis);
		}
		catch(IOException e)
		{
			throw new JRException(e);
		}

		return klexPrint;
	}


	/**
	 * @see #loadFromFile(KlexReportsContext, String)
	 */
	public static KlexPrint loadFromFile(String sourceFileName) throws JRException
	{
		return loadFromFile(DefaultKlexReportsContext.getInstance(), sourceFileName);
	}


	/**
	 * @see #loadFromFile(String)
	 */
	public static KlexPrint load(String sourceFileName) throws JRException
	{
		return loadFromFile(sourceFileName);
	}


	/**
	 *
	 */
	public static KlexPrint load(KlexReportsContext klexReportsContext, InputStream is) throws JRException
	{
		KlexPrint klexPrint = null;

		JRPrintXmlLoader printXmlLoader = new JRPrintXmlLoader(klexReportsContext);
		klexPrint = printXmlLoader.loadXML(is);

		return klexPrint;
	}


	/**
	 * @see #load(KlexReportsContext, InputStream)
	 */
	public static KlexPrint load(InputStream is) throws JRException
	{
		return load(DefaultKlexReportsContext.getInstance(), is);
	}


	/**
	 *
	 */
	private KlexPrint loadXML(InputStream is) throws JRException
	{
		PrintXmlLoader loader = new PrintXmlLoader();
		return loader.load(is);
	}

}
