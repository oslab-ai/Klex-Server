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
package net.sf.klexreports.engine;

import java.awt.Image;
import java.io.InputStream;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.export.JRPrintServiceExporter;
import net.sf.klexreports.engine.print.JRPrinterAWT;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * Facade class for the printing functionality exposed by the KlexReports library.
 * <p>
 * After having filled a report, you have the option of viewing it, exporting it to a different
 * format, or (most commonly) printing it.
 * <p>
 * In KlexReports, you can print reports using this manager class. It contains various methods that
 * can send entire documents or portions of them to the printer. It also allows people to choose
 * whether to display the print dialog. one can display the content of a page from a
 * KlexReports document by generating a <code>java.awt.Image</code> object for it using this
 * manager class.
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class KlexPrintManager
{
	public static final String EXCEPTION_MESSAGE_KEY_NO_AVAILABLE_PRINTER = "print.no.available.printer";
	
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private KlexPrintManager(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	private static KlexPrintManager getDefaultInstance()
	{
		return new KlexPrintManager(DefaultKlexReportsContext.getInstance());
	}
	
	
	/**
	 *
	 */
	public static KlexPrintManager getInstance(KlexReportsContext klexReportsContext)
	{
		return new KlexPrintManager(klexReportsContext);
	}
	
	
	/**
	 *
	 */
	public boolean print(
		String sourceFileName,
		boolean withPrintDialog
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(sourceFileName);

		return print(klexPrint, withPrintDialog);
	}


	/**
	 *
	 */
	public boolean print(
		InputStream inputStream,
		boolean withPrintDialog
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(inputStream);

		return print(klexPrint, withPrintDialog);
	}


	/**
	 *
	 */
	public boolean print(
		KlexPrint klexPrint,
		boolean withPrintDialog
		) throws JRException
	{
		//artf1936
		boolean checkAvailablePrinters = JRPropertiesUtil.getInstance(klexReportsContext).getBooleanProperty(klexPrint, PROPERTY_CHECK_AVAILABLE_PRINTERS, true);
		if (checkAvailablePrinters && !JRPrintServiceExporter.checkAvailablePrinters()) 
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_NO_AVAILABLE_PRINTER,
					(Object[])null);
		}
		//END - artf1936
		
		return 
			print(
				klexPrint,
				0,
				klexPrint.getPages().size() - 1,
				withPrintDialog
				);
	}


	/**
	 *
	 */
	public boolean print(
		String sourceFileName,
		int pageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(sourceFileName);

		return print(klexPrint, pageIndex, withPrintDialog);
	}


	/**
	 *
	 */
	public boolean print(
		InputStream inputStream,
		int pageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(inputStream);

		return print(klexPrint, pageIndex, withPrintDialog);
	}


	/**
	 *
	 */
	public boolean print(
		KlexPrint klexPrint,
		int pageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return 
			print(
				klexPrint,
				pageIndex,
				pageIndex,
				withPrintDialog
				);
	}


	/**
	 *
	 */
	public boolean print(
		String sourceFileName,
		int firstPageIndex,
		int lastPageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(sourceFileName);

		return 
			print(
				klexPrint,
				firstPageIndex,
				lastPageIndex,
				withPrintDialog
				);
	}


	/**
	 *
	 */
	public boolean print(
		InputStream inputStream,
		int firstPageIndex,
		int lastPageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(inputStream);

		return 
			print(
				klexPrint,
				firstPageIndex,
				lastPageIndex,
				withPrintDialog
				);
	}


	/**
	 *
	 */
	public boolean print(
		KlexPrint klexPrint,
		int firstPageIndex,
		int lastPageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return 
			new JRPrinterAWT(klexReportsContext, klexPrint).printPages(
				firstPageIndex,
				lastPageIndex,
				withPrintDialog
				);
	}


	/**
	 *
	 */
	public Image printToImage(
		String sourceFileName,
		int pageIndex,
		float zoom
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(sourceFileName);

		return printToImage(klexPrint, pageIndex, zoom);
	}


	/**
	 *
	 */
	public Image printToImage(
		InputStream inputStream,
		int pageIndex,
		float zoom
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(inputStream);

		return printToImage(klexPrint, pageIndex, zoom);
	}


	/**
	 *
	 */
	public Image printToImage(
		KlexPrint klexPrint,
		int pageIndex,
		float zoom
		) throws JRException
	{
		return new JRPrinterAWT(klexReportsContext, klexPrint).printPageToImage(pageIndex, zoom);
	}


	/**
	 * @see #print(String, boolean)
	 */
	public static boolean printReport(
		String sourceFileName,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(sourceFileName, withPrintDialog);
	}


	/**
	 * @see #print(InputStream, boolean)
	 */
	public static boolean printReport(
		InputStream inputStream,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(inputStream, withPrintDialog);
	}


	/**
	 * @see #print(KlexPrint, boolean)
	 */
	public static boolean printReport(
		KlexPrint klexPrint,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(klexPrint, withPrintDialog);
	}


	/**
	 * @see #print(String, int, boolean)
	 */
	public static boolean printPage(
		String sourceFileName,
		int pageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(sourceFileName, pageIndex, withPrintDialog);
	}


	/**
	 * @see #print(InputStream, int, boolean)
	 */
	public static boolean printPage(
		InputStream inputStream,
		int pageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(inputStream, pageIndex, withPrintDialog);
	}


	/**
	 * @see #print(KlexPrint, int, boolean)
	 */
	public static boolean printPage(
		KlexPrint klexPrint,
		int pageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(klexPrint, pageIndex, withPrintDialog);
	}


	/**
	 * @see #print(String, int, int, boolean)
	 */
	public static boolean printPages(
		String sourceFileName,
		int firstPageIndex,
		int lastPageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(sourceFileName, firstPageIndex, lastPageIndex, withPrintDialog);
	}


	/**
	 * @see #print(InputStream, int, int, boolean)
	 */
	public static boolean printPages(
		InputStream inputStream,
		int firstPageIndex,
		int lastPageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(inputStream, firstPageIndex, lastPageIndex, withPrintDialog);
	}


	/**
	 * @see #print(KlexPrint, int, int, boolean)
	 */
	public static boolean printPages(
		KlexPrint klexPrint,
		int firstPageIndex,
		int lastPageIndex,
		boolean withPrintDialog
		) throws JRException
	{
		return getDefaultInstance().print(klexPrint, firstPageIndex, lastPageIndex, withPrintDialog); 
	}


	/**
	 * @see #printToImage(String, int, float)
	 */
	public static Image printPageToImage(
		String sourceFileName,
		int pageIndex,
		float zoom
		) throws JRException
	{
		return getDefaultInstance().printToImage(sourceFileName, pageIndex, zoom);
	}


	/**
	 * @see #printToImage(InputStream, int, float)
	 */
	public static Image printPageToImage(
		InputStream inputStream,
		int pageIndex,
		float zoom
		) throws JRException
	{
		return getDefaultInstance().printToImage(inputStream, pageIndex, zoom);
	}


	/**
	 * @see #printToImage(KlexPrint, int, float)
	 */
	public static Image printPageToImage(
		KlexPrint klexPrint,
		int pageIndex,
		float zoom
		) throws JRException
	{
		return getDefaultInstance().printToImage(klexPrint, pageIndex, zoom);
	}


	/**
	 * Property whose value is used to check the availability of printers accepting jobs.
	 * <p/>
	 * This property is by default set to <code>true</code>.
	 */
	@Property(
			valueType = Boolean.class,
			defaultValue = PropertyConstants.BOOLEAN_TRUE,
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT},
			sinceVersion = PropertyConstants.VERSION_3_7_3
			)
	public static final String PROPERTY_CHECK_AVAILABLE_PRINTERS = JRPropertiesUtil.PROPERTY_PREFIX + "awt.check.available.printers";
}
