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
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.KlexRunManager;
import net.sf.klexreports.engine.export.JRCsvExporter;
import net.sf.klexreports.engine.export.JRCsvMetadataExporter;
import net.sf.klexreports.engine.export.JRRtfExporter;
import net.sf.klexreports.engine.export.oasis.JROdsExporter;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.export.ooxml.JRDocxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.export.ooxml.XlsxMetadataExporter;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.export.SimpleXlsMetadataReportConfiguration;
import net.sf.klexreports.export.SimpleXlsReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxMetadataReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxReportConfiguration;
import net.sf.klexreports.json.export.JsonMetadataExporter;
import net.sf.klexreports.pdf.JRPdfExporter;
import net.sf.klexreports.pdf.SimplePdfExporterConfiguration;
import net.sf.klexreports.pdf.type.PdfaConformanceEnum;
import net.sf.klexreports.poi.export.JRXlsExporter;
import net.sf.klexreports.poi.export.JRXlsMetadataExporter;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class KlexApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args) 
	{
		main(new KlexApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		pdf();
		pdfa1();
		xmlEmbed();
		xml();
		html();
		rtf();
		csv();
		csvMetadata();
		jsonMetadata();
		odt();
		ods();
		docx();
		pptx();
		xls();
		xlsMetadata();
		xlsx();
		xlsxMetadata();

	}
	
	
	/**
	 *
	 */
	public void fill() throws JRException
	{
		long start = System.currentTimeMillis();
		//Preparing parameters
		Image image = 
			Toolkit.getDefaultToolkit().createImage(
				JRLoader.loadBytesFromResource("dukesign.jpg")
				);
		MediaTracker traker = new MediaTracker(new Panel());
		traker.addImage(image, 0);
		try
		{
			traker.waitForID(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ReportTitle", "The First Klex Report Ever");
		parameters.put("MaxOrderID", 10500);
		parameters.put("SummaryImage", image);
		
		KlexFillManager.fillReportToFile("target/reports/FirstKlex.klex", parameters, getDemoHsqldbConnection());
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void print() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexPrintManager.printReport("target/reports/FirstKlex.jrprint", true);
		System.err.println("Printing time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void pdf() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToPdfFile("target/reports/FirstKlex.jrprint");
		System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 * 
	 */
	public void pdfa1() throws JRException
	{
		long start = System.currentTimeMillis();

		try
		{
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			JRPdfExporter exporter = new JRPdfExporter();
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
			
			KlexPrint jp = (KlexPrint)JRLoader.loadObject(new File("target/reports/FirstKlex.jrprint"));
			
			// Exclude transparent images when exporting to PDF; elements marked with the key 'TransparentImage'
			// will be excluded from the exported PDF
			jp.setProperty("net.sf.klexreports.export.pdf.exclude.key.TransparentImage", null);
			
			exporter.setExporterInput(new SimpleExporterInput(jp));
			
			SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
			
			// Include structure tags for PDF/A-1a compliance; unnecessary for PDF/A-1b
			configuration.setTagged(true);
			
			configuration.setPdfaConformance(PdfaConformanceEnum.PDFA_1A);
			
			// Specify the path for the ICC profile
			configuration.setIccProfilePath("./sRGB_IEC61966-2-1_no_black_scaling.icc");
			
			exporter.setConfiguration(configuration);
			exporter.exportReport();

			FileOutputStream fos = new FileOutputStream("target/reports/FirstKlex_pdfa.pdf");
			os.writeTo(fos);
			fos.close();
		}
		catch(Exception e)
		{
			 e.printStackTrace();
		}
				
		System.err.println("PDF/A-1a creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xml() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile("target/reports/FirstKlex.jrprint", false);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xmlEmbed() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile("target/reports/FirstKlex.jrprint", true);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void html() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToHtmlFile("target/reports/FirstKlex.jrprint");
		System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void rtf() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

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
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);
		
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xls");
		
		Map<String, String> dateFormats = new HashMap<String, String>();
		dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");

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
	public void xlsMetadata() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".metadata.xls");

		Map<String, String> dateFormats = new HashMap<String, String>();
		dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");

		JRXlsMetadataExporter exporter = new JRXlsMetadataExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		SimpleXlsMetadataReportConfiguration configuration = new SimpleXlsMetadataReportConfiguration();
		configuration.setOnePagePerSheet(true);
		configuration.setDetectCellType(true);
		configuration.setFormatPatternsMap(dateFormats);
		exporter.setConfiguration(configuration);

		exporter.exportReport();

		System.err.println("Metadata XLS creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xlsxMetadata() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");
		
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);
		
		File destFile1 = new File(sourceFile.getParent(), klexPrint.getName() + ".multiSheet.metadata.xlsx");
		File destFile2 = new File(sourceFile.getParent(), klexPrint.getName() + ".singleSheet.metadata.xlsx");
		
		Map<String, String> dateFormats = new HashMap<String, String>();
		dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");
		
		long start1 = System.currentTimeMillis();
		XlsxMetadataExporter exporter1 = new XlsxMetadataExporter();
		
		exporter1.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter1.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile1));
		SimpleXlsxMetadataReportConfiguration configuration1 = new SimpleXlsxMetadataReportConfiguration();
		configuration1.setOnePagePerSheet(true);
		configuration1.setDetectCellType(true);
		configuration1.setFormatPatternsMap(dateFormats);
		exporter1.setConfiguration(configuration1);
		
		exporter1.exportReport();
		
		System.err.println("Metadata multiSheet XLSX creation time : " + (System.currentTimeMillis() - start1));
		
		long start2 = System.currentTimeMillis();
		XlsxMetadataExporter exporter2 = new XlsxMetadataExporter();
		
		exporter2.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter2.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile2));
		SimpleXlsxMetadataReportConfiguration configuration2 = new SimpleXlsxMetadataReportConfiguration();
		configuration2.setDetectCellType(true);
		configuration2.setFormatPatternsMap(dateFormats);
		exporter2.setConfiguration(configuration2);
		
		exporter2.exportReport();
		
		System.err.println("Metadata singleSheet XLSX creation time : " + (System.currentTimeMillis() - start2));
		
		System.err.println("Metadata XLSX creation time : " + (System.currentTimeMillis() - start));
		
	}
	
	
	/**
	 *
	 */
	public void jsonMetadata() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".json");

		JsonMetadataExporter exporter = new JsonMetadataExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));

		exporter.exportReport();

		System.err.println("Metadata JSON creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void csv() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

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
	public void csvMetadata() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".metadata.csv");
		
		JRCsvMetadataExporter exporter = new JRCsvMetadataExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));
		
		exporter.exportReport();

		System.err.println("Metadata CSV creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void odt() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

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
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".ods");

		JROdsExporter exporter = new JROdsExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

		exporter.exportReport();

		System.err.println("ODS creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void docx() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

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
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xlsx");

		Map<String, String> dateFormats = new HashMap<String, String>();
		dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");

		JRXlsxExporter exporter = new JRXlsxExporter();

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
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
		File sourceFile = new File("target/reports/FirstKlex.jrprint");

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
	public void run() throws JRException
	{
		long start = System.currentTimeMillis();
		//Preparing parameters
		Image image = Toolkit.getDefaultToolkit().createImage("dukesign.jpg");
		MediaTracker traker = new MediaTracker(new Panel());
		traker.addImage(image, 0);
		try
		{
			traker.waitForID(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ReportTitle", "The First Klex Report Ever");
		parameters.put("MaxOrderID", 10500);
		parameters.put("SummaryImage", image);
		
		KlexRunManager.runReportToPdfFile("target/reports/FirstKlex.klex", parameters, getDemoHsqldbConnection());
		System.err.println("PDF running time : " + (System.currentTimeMillis() - start));
	}


}
