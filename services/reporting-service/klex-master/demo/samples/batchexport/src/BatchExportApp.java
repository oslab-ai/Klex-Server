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

import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.engine.JREmptyDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.export.HtmlExporter;
import net.sf.klexreports.engine.export.JRCsvExporter;
import net.sf.klexreports.engine.export.JRRtfExporter;
import net.sf.klexreports.engine.export.oasis.JROdsExporter;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.export.ooxml.JRDocxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleHtmlExporterOutput;
import net.sf.klexreports.export.SimpleOdsReportConfiguration;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.export.SimpleXlsReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxReportConfiguration;
import net.sf.klexreports.pdf.JRPdfExporter;
import net.sf.klexreports.pdf.SimplePdfExporterConfiguration;
import net.sf.klexreports.poi.export.JRXlsExporter;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class BatchExportApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new BatchExportApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		pdf();
		html();
		rtf();
		xls();
		csv();
		odt();
		ods();
		docx();
		xlsx();
		pptx();
	}


	/**
	 *
	 */
	public void fill() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexFillManager.fillReportToFile(
			"target/reports/Report1.klex",
			null, 
			new JREmptyDataSource(2)
			);
		KlexFillManager.fillReportToFile(
			"target/reports/Report2.klex",
			null, 
			new JREmptyDataSource(2)
			);
		KlexFillManager.fillReportToFile(
			"target/reports/Report3.klex",
			null, 
			new JREmptyDataSource(2)
			);
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void pdf() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JRPdfExporter exporter = new JRPdfExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("target/reports/BatchExportReport.pdf"));
		SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
		configuration.setCreatingBatchModeBookmarks(true);
		exporter.setConfiguration(configuration);
		
		exporter.exportReport();
		
		System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void html() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		HtmlExporter exporter = new HtmlExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleHtmlExporterOutput("target/reports/BatchExportReport.html"));
		
		exporter.exportReport();
		
		System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void rtf() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JRRtfExporter exporter = new JRRtfExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleWriterExporterOutput("target/reports/BatchExportReport.rtf"));
		
		exporter.exportReport();

		System.err.println("RTF creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xls() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JRXlsExporter exporter = new JRXlsExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("target/reports/BatchExportReport.xls"));
		SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
		configuration.setOnePagePerSheet(false);
		exporter.setConfiguration(configuration);
		
		exporter.exportReport();

		System.err.println("XLS creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void csv() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JRCsvExporter exporter = new JRCsvExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleWriterExporterOutput("target/reports/BatchExportReport.csv"));
		
		exporter.exportReport();

		System.err.println("CSV creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void odt() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JROdtExporter exporter = new JROdtExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("target/reports/BatchExportReport.odt"));
		
		exporter.exportReport();

		System.err.println("ODT creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void ods() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JROdsExporter exporter = new JROdsExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("target/reports/BatchExportReport.ods"));
		SimpleOdsReportConfiguration configuration = new SimpleOdsReportConfiguration();
		configuration.setOnePagePerSheet(false);
		exporter.setConfiguration(configuration);
		
		exporter.exportReport();

		System.err.println("ODS creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void docx() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JRDocxExporter exporter = new JRDocxExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("target/reports/BatchExportReport.docx"));
		
		exporter.exportReport();

		System.err.println("DOCX creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xlsx() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JRXlsxExporter exporter = new JRXlsxExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("target/reports/BatchExportReport.xlsx"));
		SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
		configuration.setOnePagePerSheet(false);
		exporter.setConfiguration(configuration);
		
		exporter.exportReport();

		System.err.println("XLSX creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void pptx() throws JRException
	{
		long start = System.currentTimeMillis();
		List<KlexPrint> klexPrintList = new ArrayList<KlexPrint>();
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report1.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report2.jrprint"));
		klexPrintList.add((KlexPrint)JRLoader.loadObjectFromFile("target/reports/Report3.jrprint"));
		
		JRPptxExporter exporter = new JRPptxExporter();
		
		exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("target/reports/BatchExportReport.pptx"));
		
		exporter.exportReport();

		System.err.println("PPTX creation time : " + (System.currentTimeMillis() - start));
	}
	
	
}
