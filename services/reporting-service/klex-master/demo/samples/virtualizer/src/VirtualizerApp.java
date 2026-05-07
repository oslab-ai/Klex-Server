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

import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JREmptyDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.export.JRCsvExporter;
import net.sf.klexreports.engine.fill.JRFileVirtualizer;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.view.KlexViewer;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class VirtualizerApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args) 
	{
		main(new VirtualizerApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		export();
	}
	
	
	/**
	 *
	 */
	public void view() throws JRException
	{
		KlexPrint klexPrint = fillReport();

		KlexViewer.viewReport(klexPrint, true);
	}
	
	
	/**
	 *
	 */
	public void print() throws JRException
	{
		KlexPrint klexPrint = fillReport();

		KlexPrintManager.printReport(klexPrint, true);
	}
	
	
	/**
	 *
	 */
	public void pdf() throws JRException
	{
		KlexPrint klexPrint = fillReport();

		exportPdf(klexPrint);
	}
	
	
	/**
	 *
	 */
	public void xml() throws JRException
	{
		KlexPrint klexPrint = fillReport();

		exportXml(klexPrint, false);
	}
	
	
	/**
	 *
	 */
	public void xmlEmbed() throws JRException
	{
		KlexPrint klexPrint = fillReport();

		exportXml(klexPrint, true);
	}
	
	
	/**
	 *
	 */
	public void csv() throws JRException
	{
		KlexPrint klexPrint = fillReport();

		exportCsv(klexPrint);
	}
	
	
	/**
	 *
	 */
	public void export() throws JRException
	{
		// creating the virtualizer
		JRFileVirtualizer virtualizer = new JRFileVirtualizer(2, "tmp");

		KlexPrint klexPrint = fillReport(virtualizer);

		exportPdf(klexPrint);
		exportXml(klexPrint, false);
		exportHtml(klexPrint);
		exportCsv(klexPrint);
		
		// manually cleaning up
		virtualizer.cleanup();
	}
	

	private static KlexPrint fillReport() throws JRException
	{
		// creating the virtualizer
		JRFileVirtualizer virtualizer = new JRFileVirtualizer(2, "tmp");
		
		return fillReport(virtualizer);
	}


	private static KlexPrint fillReport(JRFileVirtualizer virtualizer) throws JRException
	{
		long start = System.currentTimeMillis();

		// Virtualization works only with in memory KlexPrint objects.
		// All the operations will first fill the report and then export
		// the filled object.
		
		// creating the data source
		JRDataSource dataSource = new JREmptyDataSource(1000);
		
		// Preparing parameters
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);

		// filling the report
		KlexPrint klexPrint = KlexFillManager.fillReport("target/reports/VirtualizerReport.klex", parameters, dataSource);
		
		virtualizer.setReadOnly(true);

		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
		return klexPrint;
	}


	private static void exportCsv(KlexPrint klexPrint) throws JRException
	{
		long start = System.currentTimeMillis();
		JRCsvExporter exporter = new JRCsvExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleWriterExporterOutput("target/reports/" + klexPrint.getName() + ".csv"));

		exporter.exportReport();

		System.err.println("CSV creation time : " + (System.currentTimeMillis() - start));
	}

	
	private static void exportHtml(KlexPrint klexPrint) throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToHtmlFile(klexPrint, "target/reports/" + klexPrint.getName() + ".html");
		System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
	}

	
	private static void exportXml(KlexPrint klexPrint, boolean embedded) throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile(klexPrint, "target/reports/" + klexPrint.getName() + ".jrpxml", embedded);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}

	
	private static void exportPdf(KlexPrint klexPrint) throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToPdfFile(klexPrint, "target/reports/" + klexPrint.getName() + ".pdf");
		System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
	}

}
