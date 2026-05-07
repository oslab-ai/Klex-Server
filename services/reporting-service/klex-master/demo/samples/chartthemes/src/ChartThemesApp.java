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
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.chartthemes.simple.XmlChartTheme;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.data.JRCsvDataSource;
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
import net.sf.klexreports.export.SimpleOdsReportConfiguration;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.export.SimpleXlsReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxReportConfiguration;
import net.sf.klexreports.poi.export.JRXlsExporter;
import themes.AegeanSettingsFactory;
import themes.EyeCandySixtiesSettingsFactory;
import themes.SimpleSettingsFactory;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class ChartThemesApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new ChartThemesApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		pdf();
		xmlEmbed();
		xml();
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
	public void themes() throws JRException
	{
		long start = System.currentTimeMillis();
		new File("build/themes").mkdirs();
		XmlChartTheme.saveSettings(
			SimpleSettingsFactory.createChartThemeSettings(), 
			new File("build/themes/simple.jrctx")
			);
		XmlChartTheme.saveSettings(
			EyeCandySixtiesSettingsFactory.createChartThemeSettings(), 
			new File("build/themes/eye.candy.sixties.jrctx")
			);
		XmlChartTheme.saveSettings(
			AegeanSettingsFactory.createChartThemeSettings(), 
			new File("build/themes/aegean.jrctx")
			);
		System.err.println("Theme saving time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void fill() throws JRException
	{
		long start = System.currentTimeMillis();
		Map<String, Object> parameters = new HashMap<String, Object>();
		
		putDataSources(parameters);
		
		KlexFillManager.fillReportToFile("target/reports/AllChartsReport.klex", new HashMap<String, Object>(parameters));
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void print() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexPrintManager.printReport("target/reports/AllChartsReport.jrprint", true);
		System.err.println("Printing time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void pdf() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToPdfFile("target/reports/AllChartsReport.jrprint");
		System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xml() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile("target/reports/AllChartsReport.jrprint", false);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xmlEmbed() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile("target/reports/AllChartsReport.jrprint", true);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void html() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToHtmlFile("target/reports/AllChartsReport.jrprint");
		System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void rtf() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".rtf");
		
		JRRtfExporter exporter = new JRRtfExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));
		
		exporter.exportReport();

		System.err.println("RTF creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xls() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");
		Map<String, String> dateFormats = new HashMap<String, String>();
		dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xls");
		
		JRXlsExporter exporter = new JRXlsExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
		configuration.setOnePagePerSheet(true);
		configuration.setDetectCellType(true);
		configuration.setFormatPatternsMap(dateFormats);
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
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".csv");
		
		JRCsvExporter exporter = new JRCsvExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));
		
		exporter.exportReport();

		System.err.println("CSV creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void odt() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".odt");

		JROdtExporter exporter = new JROdtExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

		exporter.exportReport();

		System.err.println("ODT creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void ods() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".ods");

		JROdsExporter exporter = new JROdsExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		SimpleOdsReportConfiguration configuration = new SimpleOdsReportConfiguration();
		configuration.setOnePagePerSheet(true);
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
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".docx");

		JRDocxExporter exporter = new JRDocxExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

		exporter.exportReport();

		System.err.println("DOCX creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xlsx() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");
		Map<String, String> dateFormats = new HashMap<String, String>();
		dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xlsx");
		
		JRXlsxExporter exporter = new JRXlsxExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
		configuration.setOnePagePerSheet(true);
		configuration.setDetectCellType(true);
		configuration.setFormatPatternsMap(dateFormats);
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
		File sourceFile = new File("target/reports/AllChartsReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".pptx");
		
		JRPptxExporter exporter = new JRPptxExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

		exporter.exportReport();

		System.err.println("PPTX creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public static final void putDataSources(Map<String, Object> parameters) throws JRException
	{
		try
		{
			JRCsvDataSource cds1 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/categoryDatasource.csv"), "UTF-8");
			cds1.setRecordDelimiter("\r\n");
			cds1.setUseFirstRowAsHeader(true);
			parameters.put("categoryDatasource1", cds1);
			
			JRCsvDataSource cds2 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/categoryDatasource.csv"), "UTF-8");
			cds2.setRecordDelimiter("\r\n");
			cds2.setUseFirstRowAsHeader(true);
			parameters.put("categoryDatasource2", cds2);
			
			JRCsvDataSource cds3 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/categoryDatasource.csv"), "UTF-8");
			cds3.setRecordDelimiter("\r\n");
			cds3.setUseFirstRowAsHeader(true);
			parameters.put("categoryDatasource3", cds3);
			
			JRCsvDataSource cds4 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/categoryDatasource.csv"), "UTF-8");
			cds4.setRecordDelimiter("\r\n");
			cds4.setUseFirstRowAsHeader(true);
			parameters.put("categoryDatasource4", cds4);
			
			JRCsvDataSource cds5 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/categoryDatasource.csv"), "UTF-8");
			cds5.setRecordDelimiter("\r\n");
			cds5.setUseFirstRowAsHeader(true);
			parameters.put("categoryDatasource5", cds5);
			
			JRCsvDataSource cds6 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/categoryDatasource.csv"), "UTF-8");
			cds6.setRecordDelimiter("\r\n");
			cds6.setUseFirstRowAsHeader(true);
			parameters.put("categoryDatasource6", cds6);
			
			JRCsvDataSource cds7 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/categoryDatasource.csv"), "UTF-8");
			cds7.setRecordDelimiter("\r\n");
			cds7.setUseFirstRowAsHeader(true);
			parameters.put("categoryDatasource7", cds7);
			
			JRCsvDataSource pds1 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/pieDatasource.csv"), "UTF-8");
			pds1.setRecordDelimiter("\r\n");
			pds1.setUseFirstRowAsHeader(true);
			parameters.put("pieDatasource1", pds1);
			
			JRCsvDataSource pds2 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/pieDatasource.csv"), "UTF-8");
			pds2.setRecordDelimiter("\r\n");
			pds2.setUseFirstRowAsHeader(true);
			parameters.put("pieDatasource2", pds2);
			
			JRCsvDataSource tpds1 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/timePeriodDatasource.csv"), "UTF-8");
			tpds1.setRecordDelimiter("\r\n");
			tpds1.setUseFirstRowAsHeader(true);
			parameters.put("timePeriodDatasource1", tpds1);
			
			JRCsvDataSource tsds1 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/timeSeriesDatasource.csv"), "UTF-8");
			tsds1.setRecordDelimiter("\r\n");
			tsds1.setUseFirstRowAsHeader(true);
			tsds1.setDateFormat(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));
			parameters.put("timeSeriesDatasource1", tsds1);
			
			JRCsvDataSource tsds2 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/timeSeriesDatasource.csv"), "UTF-8");
			tsds2.setRecordDelimiter("\r\n");
			tsds2.setUseFirstRowAsHeader(true);
			tsds2.setDateFormat(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));
			parameters.put("timeSeriesDatasource2", tsds2);
			
			JRCsvDataSource tsds3 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/timeSeriesDatasource.csv"), "UTF-8");
			tsds3.setRecordDelimiter("\r\n");
			tsds3.setUseFirstRowAsHeader(true);
			tsds3.setDateFormat(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));
			parameters.put("timeSeriesDatasource3", tsds3);
			
			JRCsvDataSource xyds1 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/xyDatasource.csv"), "UTF-8");
			xyds1.setRecordDelimiter("\r\n");
			xyds1.setUseFirstRowAsHeader(true);
			parameters.put("xyDatasource1", xyds1);
			
			JRCsvDataSource xyds2 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/xyDatasource.csv"), "UTF-8");
			xyds2.setRecordDelimiter("\r\n");
			xyds2.setUseFirstRowAsHeader(true);
			parameters.put("xyDatasource2", xyds2);
			
			JRCsvDataSource xyds3 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/xyDatasource.csv"), "UTF-8");
			xyds3.setRecordDelimiter("\r\n");
			xyds3.setUseFirstRowAsHeader(true);
			parameters.put("xyDatasource3", xyds3);
			
			JRCsvDataSource xyds4 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/xyDatasource.csv"), "UTF-8");
			xyds4.setRecordDelimiter("\r\n");
			xyds4.setUseFirstRowAsHeader(true);
			parameters.put("xyDatasource4", xyds4);
			
			JRCsvDataSource xyds5 = new JRCsvDataSource(JRLoader.getLocationInputStream("data/xyDatasource.csv"), "UTF-8");
			xyds5.setRecordDelimiter("\r\n");
			xyds5.setUseFirstRowAsHeader(true);
			parameters.put("xyDatasource5", xyds5);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new JRException(e);
		}
	}

}
