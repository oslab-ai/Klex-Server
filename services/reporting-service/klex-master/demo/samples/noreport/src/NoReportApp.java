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
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.JRPrintLine;
import net.sf.klexreports.engine.JRPrintPage;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.base.JRBasePrintImage;
import net.sf.klexreports.engine.base.JRBasePrintLine;
import net.sf.klexreports.engine.base.JRBasePrintPage;
import net.sf.klexreports.engine.base.JRBasePrintText;
import net.sf.klexreports.engine.design.JRDesignStyle;
import net.sf.klexreports.engine.export.JRCsvExporter;
import net.sf.klexreports.engine.export.JRRtfExporter;
import net.sf.klexreports.engine.export.oasis.JROdsExporter;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.export.ooxml.JRDocxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.type.HorizontalTextAlignEnum;
import net.sf.klexreports.engine.type.ScaleImageEnum;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.engine.util.JRSaver;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleOdsReportConfiguration;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.export.SimpleXlsReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxReportConfiguration;
import net.sf.klexreports.renderers.ResourceRenderer;
import net.sf.klexreports.poi.export.JRXlsExporter;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class NoReportApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new NoReportApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		fill();
		pdf();
		html();
		rtf();
		xls();
		odt();
		ods();
		docx();
		xlsx();
		pptx();
		xmlEmbed();
		xml();
	}


	/**
	 *
	 */
	public void fill() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexPrint klexPrint = getKlexPrint();
		File file = new File("target/reports/NoReport.jrprint");
		file.getParentFile().mkdirs();
		JRSaver.saveObject(klexPrint, file);
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void print() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexPrintManager.printReport("target/reports/NoReport.jrprint", true);
		System.err.println("Printing time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void pdf() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToPdfFile("target/reports/NoReport.jrprint");
		System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void rtf() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/NoReport.jrprint");

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
	public void xml() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile("target/reports/NoReport.jrprint", false);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void xmlEmbed() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile("target/reports/NoReport.jrprint", true);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void html() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToHtmlFile("target/reports/NoReport.jrprint");
		System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void xls() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/NoReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xls");
		
		JRXlsExporter exporter = new JRXlsExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
		configuration.setOnePagePerSheet(true);
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
		File sourceFile = new File("target/reports/NoReport.jrprint");

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
		File sourceFile = new File("target/reports/NoReport.jrprint");

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
		File sourceFile = new File("target/reports/NoReport.jrprint");

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
		File sourceFile = new File("target/reports/NoReport.jrprint");

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
		File sourceFile = new File("target/reports/NoReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xlsx");
		
		JRXlsxExporter exporter = new JRXlsxExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
		configuration.setOnePagePerSheet(true);
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
		File sourceFile = new File("target/reports/NoReport.jrprint");

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
	private static KlexPrint getKlexPrint() throws JRException
	{
		//KlexPrint
		KlexPrint klexPrint = new KlexPrint();
		klexPrint.setName("NoReport");
		klexPrint.setPageWidth(595);
		klexPrint.setPageHeight(842);
		
		//Fonts
		JRDesignStyle normalStyle = new JRDesignStyle();
		normalStyle.setName("Sans_Normal");
		normalStyle.setDefault(true);
		normalStyle.setFontName("DejaVu Sans");
		normalStyle.setFontSize(8f);
		normalStyle.setPdfFontName("Helvetica");
		normalStyle.setPdfEncoding("Cp1252");
		normalStyle.setPdfEmbedded(Boolean.FALSE);
		klexPrint.addStyle(normalStyle);

		JRDesignStyle boldStyle = new JRDesignStyle();
		boldStyle.setName("Sans_Bold");
		boldStyle.setFontName("DejaVu Sans");
		boldStyle.setFontSize(8f);
		boldStyle.setBold(Boolean.TRUE);
		boldStyle.setPdfFontName("Helvetica-Bold");
		boldStyle.setPdfEncoding("Cp1252");
		boldStyle.setPdfEmbedded(Boolean.FALSE);
		klexPrint.addStyle(boldStyle);

		JRDesignStyle italicStyle = new JRDesignStyle();
		italicStyle.setName("Sans_Italic");
		italicStyle.setFontName("DejaVu Sans");
		italicStyle.setFontSize(8f);
		italicStyle.setItalic(Boolean.TRUE);
		italicStyle.setPdfFontName("Helvetica-Oblique");
		italicStyle.setPdfEncoding("Cp1252");
		italicStyle.setPdfEmbedded(Boolean.FALSE);
		klexPrint.addStyle(italicStyle);
		
		JRPrintPage page = new JRBasePrintPage();

		JRPrintLine line = new JRBasePrintLine(klexPrint.getDefaultStyleProvider());
		line.setX(40);
		line.setY(50);
		line.setWidth(515);
		line.setHeight(0);
		page.addElement(line);
		
		JRPrintImage image = new JRBasePrintImage(klexPrint.getDefaultStyleProvider());
		image.setX(45);
		image.setY(55);
		image.setWidth(165);
		image.setHeight(40);
		image.setScaleImage(ScaleImageEnum.CLIP);
		image.setRenderer(
			ResourceRenderer.getInstance("klexreports.png", false)
			);
		page.addElement(image);

		JRPrintText text = new JRBasePrintText(klexPrint.getDefaultStyleProvider());
		text.setX(210);
		text.setY(55);
		text.setWidth(345);
		text.setHeight(30);
		text.setTextHeight(text.getHeight());
		text.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
		text.setLineSpacingFactor(1.3133681f);
		text.setLeadingOffset(-4.955078f);
		text.setStyle(boldStyle);
		text.setFontSize(18f);
		text.setText("KlexReports Project Description");
		page.addElement(text);

		text = new JRBasePrintText(klexPrint.getDefaultStyleProvider());
		text.setX(210);
		text.setY(85);
		text.setWidth(325);
		text.setHeight(15);
		text.setTextHeight(text.getHeight());
		text.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
		text.setLineSpacingFactor(1.329241f);
		text.setLeadingOffset(-4.076172f);
		text.setStyle(italicStyle);
		text.setFontSize(12f);
		text.setText((new SimpleDateFormat("EEE, MMM d, yyyy")).format(new Date()));
		page.addElement(text);

		text = new JRBasePrintText(klexPrint.getDefaultStyleProvider());
		text.setX(40);
		text.setY(150);
		text.setWidth(515);
		text.setHeight(200);
		text.setTextHeight(text.getHeight());
		text.setHorizontalTextAlign(HorizontalTextAlignEnum.JUSTIFIED);
		text.setLineSpacingFactor(1.329241f);
		text.setLeadingOffset(-4.076172f);
		text.setStyle(normalStyle);
		text.setFontSize(14f);
		text.setText(
			"KlexReports is a powerful report-generating tool that has the ability to deliver rich content onto the screen, to the printer or into PDF, HTML, XLS, CSV or XML files.\n\n" +
			"It is entirely written in Java and can be used in a variety of Java enabled applications, including J2EE or Web applications, to generate dynamic content.\n\n" +
			"Its main purpose is to help creating page oriented, ready to print documents in a simple and flexible manner."
			);
		page.addElement(text);

		klexPrint.addPage(page);

		return klexPrint;
	}
	

}
