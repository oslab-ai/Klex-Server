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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Map;

import net.sf.klexreports.engine.fill.JRFiller;


/**
 * Facade class for the KlexReports engine. 
 * <p>
 * Sometimes it is useful to produce documents only in a popular format such as PDF or
 * HTML, without having to store on disk the serialized, intermediate
 * {@link net.sf.klexreports.engine.KlexPrint} object produced by the report-filling
 * process.
 * </p><p>
 * This can be achieved using this manager class, which immediately exports the document
 * produced by the report-filling process into the desired output format.
 * </p>
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class KlexRunManager
{
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private KlexRunManager(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	private static KlexRunManager getDefaultInstance()
	{
		return new KlexRunManager(DefaultKlexReportsContext.getInstance());
	}
	
	
	/**
	 *
	 */
	public static KlexRunManager getInstance(KlexReportsContext klexReportsContext)
	{
		return new KlexRunManager(klexReportsContext);
	}
	
	
	/**
	 * Fills a report and saves it directly into a PDF file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public String runToPdfFile(
		String sourceFileName, 
		Map<String,Object> params, 
		Connection conn
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/*   */
		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				KlexFillManager.getReportSource(klexReportsContext, sourceFile), 
				params, conn);

		/*   */
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".pdf");
		String destFileName = destFile.toString();

		KlexExportManager.getInstance(klexReportsContext).exportToPdfFile(klexPrint, destFileName);
		
		return destFileName;
	}


	/**
	 * Fills a report and saves it directly into a PDF file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param sourceFileName the name of the compiled report file
	 * @param params the parameters map
	 * @return the name of the generated PDF file
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public String runToPdfFile(
		String sourceFileName, 
		Map<String,Object> params 
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/*   */
		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				KlexFillManager.getReportSource(klexReportsContext, sourceFile), 
				params);

		/*   */
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".pdf");
		String destFileName = destFile.toString();

		KlexExportManager.getInstance(klexReportsContext).exportToPdfFile(klexPrint, destFileName);
		
		return destFileName;
	}

	
	/**
	 * Fills a report and saves it directly into a PDF file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public void runToPdfFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters, conn);

		KlexExportManager.getInstance(klexReportsContext).exportToPdfFile(klexPrint, destFileName);
	}


	/**
	 * Fills a report and saves it directly into a PDF file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param destFileName PDF destination file name
	 * @param parameters     report parameters map
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public void runToPdfFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters 
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters);

		KlexExportManager.getInstance(klexReportsContext).exportToPdfFile(klexPrint, destFileName);
	}

	
	/**
	 * Fills a report and sends it directly to an OutputStream in PDF format. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public void runToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(inputStream, parameters, conn);

		KlexExportManager.getInstance(klexReportsContext).exportToPdfStream(klexPrint, outputStream);
	}


	/**
	 * Fills a report and sends it directly to an OutputStream in PDF format. 
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param inputStream compiled report input stream
	 * @param outputStream PDF output stream
	 * @param parameters parameters map
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public void runToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters 
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(inputStream, parameters);

		KlexExportManager.getInstance(klexReportsContext).exportToPdfStream(klexPrint, outputStream);
	}

	
	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public byte[] runToPdf(
		String sourceFileName, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters, conn);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}


	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param parameters     report parameters map
	 * @return binary PDF output
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public byte[] runToPdf(
		String sourceFileName, 
		Map<String,Object> parameters 
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}

	
	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public byte[] runToPdf(
		InputStream inputStream, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(inputStream, parameters, conn);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}


	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param inputStream  input stream to read the compiled report design object from
	 * @param parameters   report parameters map
	 * @return binary PDF output
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public byte[] runToPdf(
		InputStream inputStream, 
		Map<String,Object> parameters 
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(inputStream, parameters);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}

	
	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public byte[] runToPdf(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(klexReport, parameters, conn);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}


	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param klexReport the compiled report
	 * @param parameters the parameters map
	 * @return binary PDF output
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public byte[] runToPdf(
		KlexReport klexReport, 
		Map<String,Object> parameters 
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(klexReport, parameters);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}

	
	/**
	 * Fills a report and saves it directly into a PDF file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public String runToPdfFile(
		String sourceFileName, 
		Map<String,Object> params, 
		JRDataSource jrDataSource
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/*   */
		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				KlexFillManager.getReportSource(klexReportsContext, sourceFile), 
				params, jrDataSource);

		/*   */
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".pdf");
		String destFileName = destFile.toString();

		KlexExportManager.getInstance(klexReportsContext).exportToPdfFile(klexPrint, destFileName);
		
		return destFileName;
	}

	
	/**
	 * Fills a report and saves it directly into a PDF file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public void runToPdfFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters, jrDataSource);

		/*   */
		KlexExportManager.getInstance(klexReportsContext).exportToPdfFile(klexPrint, destFileName);
	}

	
	/**
	 * Fills a report and sends it directly to an OutputStream in PDF format. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public void runToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(inputStream, parameters, jrDataSource);

		KlexExportManager.getInstance(klexReportsContext).exportToPdfStream(klexPrint, outputStream);
	}

	
	/**
	 * Fills a report and sends it to an output stream in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public byte[] runToPdf(
		String sourceFileName, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters, jrDataSource);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}

	
	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public byte[] runToPdf(
		InputStream inputStream, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(inputStream, parameters, jrDataSource);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}

	
	/**
	 * Fills a report and returns byte array object containing the report in PDF format.
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public byte[] runToPdf(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(klexReport, parameters, jrDataSource);

		return KlexExportManager.getInstance(klexReportsContext).exportToPdf(klexPrint);
	}


	/**
	 * Fills a report and saves it directly into a HTML file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public String runToHtmlFile(
		String sourceFileName, 
		Map<String,Object> params, 
		Connection conn
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/*   */
		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				KlexFillManager.getReportSource(klexReportsContext, sourceFile), 
				params, conn);

		/*   */
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".html");
		String destFileName = destFile.toString();

		KlexExportManager.getInstance(klexReportsContext).exportToHtmlFile(klexPrint, destFileName);
		
		return destFileName;
	}


	/**
	 * Fills a report and saves it directly into a HTML file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param sourceFileName the name of the compiled report file
	 * @param params the parameters map
	 * @return the name of the generated HTML file
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public String runToHtmlFile(
		String sourceFileName, 
		Map<String,Object> params 
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/*   */
		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				KlexFillManager.getReportSource(klexReportsContext, sourceFile), 
				params);

		/*   */
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".html");
		String destFileName = destFile.toString();

		KlexExportManager.getInstance(klexReportsContext).exportToHtmlFile(klexPrint, destFileName);
		
		return destFileName;
	}

	
	/**
	 * Fills a report and saves it directly into a HTML file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public void runToHtmlFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters, conn);

		KlexExportManager.getInstance(klexReportsContext).exportToHtmlFile(klexPrint, destFileName);
	}


	/**
	 * Fills a report and saves it directly into a HTML file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param destFileName name of the destination HTML file
	 * @param parameters     report parameters map
	 * @throws JRException
	 * @see net.sf.klexreports.engine.fill.JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public void runToHtmlFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters 
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters);

		KlexExportManager.getInstance(klexReportsContext).exportToHtmlFile(klexPrint, destFileName);
	}


	/**
	 * Fills a report and saves it directly into a HTML file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public String runToHtmlFile(
		String sourceFileName, 
		Map<String,Object> params, 
		JRDataSource jrDataSource
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/*   */
		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				KlexFillManager.getReportSource(klexReportsContext, sourceFile), 
				params, jrDataSource);

		/*   */
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".html");
		String destFileName = destFile.toString();

		KlexExportManager.getInstance(klexReportsContext).exportToHtmlFile(klexPrint, destFileName);
		
		return destFileName;
	}

	
	/**
	 * Fills a report and saves it directly into a HTML file. 
	 * The intermediate KlexPrint object is not saved on disk.
	 */
	public void runToHtmlFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		KlexFillManager klexFillManager = KlexFillManager.getInstance(klexReportsContext);
		
		/*   */
		KlexPrint klexPrint = klexFillManager.fill(sourceFileName, parameters, jrDataSource);

		/*   */
		KlexExportManager.getInstance(klexReportsContext).exportToHtmlFile(klexPrint, destFileName);
	}
	
	
	/**
	 * @see #runToPdfFile(String, Map, Connection)
	 */
	public static String runReportToPdfFile(
		String sourceFileName, 
		Map<String,Object> params, 
		Connection conn
		) throws JRException
	{
		return getDefaultInstance().runToPdfFile(sourceFileName, params, conn);
	}


	/**
	 * @see #runToPdfFile(String, Map)
	 */
	public static String runReportToPdfFile(
		String sourceFileName, 
		Map<String,Object> params 
		) throws JRException
	{
		return getDefaultInstance().runToPdfFile(sourceFileName, params);
	}

	
	/**
	 * @see #runToPdfFile(String, String, Map, Connection)
	 */
	public static void runReportToPdfFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		getDefaultInstance().runToPdfFile(sourceFileName, destFileName, parameters, conn);
	}


	/**
	 * @see #runToPdfFile(String, String, Map)
	 */
	public static void runReportToPdfFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters 
		) throws JRException
	{
		getDefaultInstance().runToPdfFile(sourceFileName, destFileName, parameters);
	}

	
	/**
	 * @see #runToPdfStream(InputStream, OutputStream, Map, Connection)
	 */
	public static void runReportToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		getDefaultInstance().runToPdfStream(inputStream, outputStream, parameters, conn);
	}


	/**
	 * @see #runToPdfStream(InputStream, OutputStream, Map)
	 */
	public static void runReportToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters 
		) throws JRException
	{
		getDefaultInstance().runToPdfStream(inputStream, outputStream, parameters);
	}

	
	/**
	 * @see #runToPdf(String, Map, Connection)
	 */
	public static byte[] runReportToPdf(
		String sourceFileName, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		return getDefaultInstance().runToPdf(sourceFileName, parameters, conn);
	}


	/**
	 * @see #runToPdf(String, Map)
	 */
	public static byte[] runReportToPdf(
		String sourceFileName, 
		Map<String,Object> parameters 
		) throws JRException
	{
		return getDefaultInstance().runToPdf(sourceFileName, parameters);
	}

	
	/**
	 * @see #runToPdf(InputStream, Map, Connection)
	 */
	public static byte[] runReportToPdf(
		InputStream inputStream, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		return getDefaultInstance().runToPdf(inputStream, parameters, conn);
	}


	/**
	 * @see #runToPdf(InputStream, Map)
	 */
	public static byte[] runReportToPdf(
		InputStream inputStream, 
		Map<String,Object> parameters 
		) throws JRException
	{
		return getDefaultInstance().runToPdf(inputStream, parameters);
	}

	
	/**
	 * @see #runToPdf(KlexReport, Map, Connection)
	 */
	public static byte[] runReportToPdf(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		return getDefaultInstance().runToPdf(klexReport, parameters, conn);
	}


	/**
	 * @see #runToPdf(KlexReport, Map)
	 */
	public static byte[] runReportToPdf(
		KlexReport klexReport, 
		Map<String,Object> parameters 
		) throws JRException
	{
		return getDefaultInstance().runToPdf(klexReport, parameters);
	}

	
	/**
	 * @see #runToPdfFile(String, Map, JRDataSource)
	 */
	public static String runReportToPdfFile(
		String sourceFileName, 
		Map<String,Object> params, 
		JRDataSource jrDataSource
		) throws JRException
	{
		return getDefaultInstance().runToPdfFile(sourceFileName, params, jrDataSource);
	}

	
	/**
	 * @see #runToPdfFile(String, String, Map, JRDataSource)
	 */
	public static void runReportToPdfFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		getDefaultInstance().runToPdfFile(sourceFileName, destFileName, parameters, jrDataSource);
	}

	
	/**
	 * @see #runToPdfStream(InputStream, OutputStream, Map, JRDataSource)
	 */
	public static void runReportToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		getDefaultInstance().runToPdfStream(inputStream, outputStream, parameters, jrDataSource);
	}

	
	/**
	 * @see #runToPdf(String, Map, JRDataSource)
	 */
	public static byte[] runReportToPdf(
		String sourceFileName, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		return getDefaultInstance().runToPdf(sourceFileName, parameters, jrDataSource);
	}

	
	/**
	 * @see #runToPdf(InputStream, Map, JRDataSource)
	 */
	public static byte[] runReportToPdf(
		InputStream inputStream, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		return getDefaultInstance().runToPdf(inputStream, parameters, jrDataSource);
	}

	
	/**
	 * @see #runToPdf(KlexReport, Map, JRDataSource)
	 */
	public static byte[] runReportToPdf(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		return getDefaultInstance().runToPdf(klexReport, parameters, jrDataSource);
	}


	/**
	 * @see #runToHtmlFile(String, Map, Connection)
	 */
	public static String runReportToHtmlFile(
		String sourceFileName, 
		Map<String,Object> params, 
		Connection conn
		) throws JRException
	{
		return getDefaultInstance().runToHtmlFile(sourceFileName, params, conn);
	}


	/**
	 * @see #runToHtmlFile(String, Map)
	 */
	public static String runReportToHtmlFile(
		String sourceFileName, 
		Map<String,Object> params 
		) throws JRException
	{
		return getDefaultInstance().runToHtmlFile(sourceFileName, params);
	}

	
	/**
	 * @see #runToHtmlFile(String, String, Map, Connection)
	 */
	public static void runReportToHtmlFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		Connection conn
		) throws JRException
	{
		getDefaultInstance().runToHtmlFile(sourceFileName, destFileName, parameters, conn);
	}


	/**
	 * @see #runToHtmlFile(String, String, Map)
	 */
	public static void runReportToHtmlFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters 
		) throws JRException
	{
		getDefaultInstance().runToHtmlFile(sourceFileName, destFileName, parameters);
	}


	/**
	 * @see #runToHtmlFile(String, Map, JRDataSource)
	 */
	public static String runReportToHtmlFile(
		String sourceFileName, 
		Map<String,Object> params, 
		JRDataSource jrDataSource
		) throws JRException
	{
		return getDefaultInstance().runToHtmlFile(sourceFileName, params, jrDataSource);
	}

	
	/**
	 * @see #runToHtmlFile(String, String, Map, JRDataSource)
	 */
	public static void runReportToHtmlFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> parameters, 
		JRDataSource jrDataSource
		) throws JRException
	{
		getDefaultInstance().runToHtmlFile(sourceFileName, destFileName, parameters, jrDataSource);
	}
}
