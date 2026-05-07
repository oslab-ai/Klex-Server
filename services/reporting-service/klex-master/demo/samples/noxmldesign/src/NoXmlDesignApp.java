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
import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexCompileManager;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.design.JRDesignBand;
import net.sf.klexreports.engine.design.JRDesignExpression;
import net.sf.klexreports.engine.design.JRDesignField;
import net.sf.klexreports.engine.design.JRDesignFrame;
import net.sf.klexreports.engine.design.JRDesignGroup;
import net.sf.klexreports.engine.design.JRDesignLine;
import net.sf.klexreports.engine.design.JRDesignParameter;
import net.sf.klexreports.engine.design.JRDesignQuery;
import net.sf.klexreports.engine.design.JRDesignSection;
import net.sf.klexreports.engine.design.JRDesignStaticText;
import net.sf.klexreports.engine.design.JRDesignStyle;
import net.sf.klexreports.engine.design.JRDesignTextField;
import net.sf.klexreports.engine.design.JRDesignVariable;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.export.JRCsvExporter;
import net.sf.klexreports.engine.export.JRRtfExporter;
import net.sf.klexreports.engine.export.oasis.JROdsExporter;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.export.ooxml.JRDocxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.type.CalculationEnum;
import net.sf.klexreports.engine.type.HorizontalTextAlignEnum;
import net.sf.klexreports.engine.type.ModeEnum;
import net.sf.klexreports.engine.type.PositionTypeEnum;
import net.sf.klexreports.engine.type.ResetTypeEnum;
import net.sf.klexreports.engine.type.TextAdjustEnum;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleOdsReportConfiguration;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.export.SimpleXlsReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxReportConfiguration;
import net.sf.klexreports.poi.export.JRXlsExporter;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class NoXmlDesignApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args) 
	{
		main(new NoXmlDesignApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		writeXml();
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
	
	
	@Override
	public void compile() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexDesign klexDesign = getKlexDesign();
		File file = new File("target/reports/NoXmlDesignReport.klex");
		file.getParentFile().mkdirs();
		KlexCompileManager.compileReportToFile(klexDesign, file.getPath());
		System.err.println("Compile time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void fill() throws JRException
	{
		long start = System.currentTimeMillis();
		//Preparing parameters
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ReportTitle", "Address Report");
		parameters.put("OrderByClause", "ORDER BY City");

		KlexFillManager.fillReportToFile("target/reports/NoXmlDesignReport.klex", parameters, getDemoHsqldbConnection());
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void print() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexPrintManager.printReport("target/reports/NoXmlDesignReport.jrprint", true);
		System.err.println("Printing time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void pdf() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToPdfFile("target/reports/NoXmlDesignReport.jrprint");
		System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void rtf() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

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
		KlexExportManager.exportReportToXmlFile("target/reports/NoXmlDesignReport.jrprint", false);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xmlEmbed() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToXmlFile("target/reports/NoXmlDesignReport.jrprint", true);
		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void html() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToHtmlFile("target/reports/NoXmlDesignReport.jrprint");
		System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xls() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xls");
		
		JRXlsExporter exporter = new JRXlsExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
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
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

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
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

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
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

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
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

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
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xlsx");
		
		JRXlsxExporter exporter = new JRXlsxExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
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
		File sourceFile = new File("target/reports/NoXmlDesignReport.jrprint");

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
	public void writeXml() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexCompileManager.writeReportToXmlFile("target/reports/NoXmlDesignReport.klex");
		System.err.println("XML design creation time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	private static KlexDesign getKlexDesign() throws JRException
	{
		//KlexDesign
		KlexDesign klexDesign = new KlexDesign();
		klexDesign.setName("NoXmlDesignReport");
		klexDesign.setPageWidth(595);
		klexDesign.setPageHeight(842);
		klexDesign.setColumnWidth(515);
		klexDesign.setColumnSpacing(0);
		klexDesign.setLeftMargin(40);
		klexDesign.setRightMargin(40);
		klexDesign.setTopMargin(50);
		klexDesign.setBottomMargin(50);
		
		//Fonts
		JRDesignStyle normalStyle = new JRDesignStyle();
		normalStyle.setName("Sans_Normal");
		normalStyle.setDefault(true);
		normalStyle.setFontName("DejaVu Sans");
		normalStyle.setFontSize(12f);
		normalStyle.setPdfFontName("Helvetica");
		normalStyle.setPdfEncoding("Cp1252");
		normalStyle.setPdfEmbedded(Boolean.FALSE);
		klexDesign.addStyle(normalStyle);

		JRDesignStyle boldStyle = new JRDesignStyle();
		boldStyle.setName("Sans_Bold");
		boldStyle.setFontName("DejaVu Sans");
		boldStyle.setFontSize(12f);
		boldStyle.setBold(Boolean.TRUE);
		boldStyle.setPdfFontName("Helvetica-Bold");
		boldStyle.setPdfEncoding("Cp1252");
		boldStyle.setPdfEmbedded(Boolean.FALSE);
		klexDesign.addStyle(boldStyle);

		JRDesignStyle italicStyle = new JRDesignStyle();
		italicStyle.setName("Sans_Italic");
		italicStyle.setFontName("DejaVu Sans");
		italicStyle.setFontSize(12f);
		italicStyle.setItalic(Boolean.TRUE);
		italicStyle.setPdfFontName("Helvetica-Oblique");
		italicStyle.setPdfEncoding("Cp1252");
		italicStyle.setPdfEmbedded(Boolean.FALSE);
		klexDesign.addStyle(italicStyle);
		
		//Parameters
		JRDesignParameter parameter = new JRDesignParameter();
		parameter.setName("ReportTitle");
		parameter.setValueClass(java.lang.String.class);
		klexDesign.addParameter(parameter);

		parameter = new JRDesignParameter();
		parameter.setName("OrderByClause");
		parameter.setValueClass(java.lang.String.class);
		klexDesign.addParameter(parameter);

		//Query
		JRDesignQuery query = new JRDesignQuery();
		query.setText("SELECT * FROM Address $P!{OrderByClause}");
		klexDesign.setQuery(query);

		//Fields
		JRDesignField field = new JRDesignField();
		field.setName("Id");
		field.setValueClass(java.lang.Integer.class);
		klexDesign.addField(field);

		field = new JRDesignField();
		field.setName("FirstName");
		field.setValueClass(java.lang.String.class);
		klexDesign.addField(field);

		field = new JRDesignField();
		field.setName("LastName");
		field.setValueClass(java.lang.String.class);
		klexDesign.addField(field);

		field = new JRDesignField();
		field.setName("Street");
		field.setValueClass(java.lang.String.class);
		klexDesign.addField(field);

		field = new JRDesignField();
		field.setName("City");
		field.setValueClass(java.lang.String.class);
		klexDesign.addField(field);

		//Variables
		JRDesignVariable variable = new JRDesignVariable();
		variable.setName("CityNumber");
		variable.setValueClass(java.lang.Integer.class);
		variable.setResetType(ResetTypeEnum.GROUP);
		JRDesignGroup group = new JRDesignGroup();
		group.setName("CityGroup");
		variable.setResetGroup(group.getName());
		variable.setCalculation(CalculationEnum.SYSTEM);
		variable.setInitialValueExpression(new JRDesignExpression("$V{CityNumber} == null ? 1 : ($V{CityNumber} + 1)"));
		klexDesign.addVariable(variable);

		variable = new JRDesignVariable();
		variable.setName("AllCities");
		variable.setValueClass(java.lang.String.class);
		variable.setResetType(ResetTypeEnum.REPORT);
		variable.setCalculation(CalculationEnum.SYSTEM);
		klexDesign.addVariable(variable);

		//Groups
		group.setMinHeightToStartNewPage(60);
		group.setExpression(new JRDesignExpression("$F{City}"));

		JRDesignBand band = new JRDesignBand();
		band.setHeight(20);
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(0);
		textField.setY(4);
		textField.setWidth(515);
		textField.setHeight(15);
		textField.setBackcolor(new Color(0xC0, 0xC0, 0xC0));
		textField.setMode(ModeEnum.OPAQUE);
		textField.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
		textField.setStyle(boldStyle);
		textField.setExpression(new JRDesignExpression("\"  \" + String.valueOf($V{CityNumber}) + \". \" + String.valueOf($F{City})"));
		band.addElement(textField);
		JRDesignLine line = new JRDesignLine();
		line.setX(0);
		line.setY(19);
		line.setWidth(515);
		line.setHeight(0);
		band.addElement(line);
		((JRDesignSection)group.getGroupHeaderSection()).addBand(band);

		band = new JRDesignBand();
		band.setHeight(20);
		line = new JRDesignLine();
		line.setX(0);
		line.setY(-1);
		line.setWidth(515);
		line.setHeight(0);
		band.addElement(line);
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(400);
		staticText.setY(0);
		staticText.setWidth(60);
		staticText.setHeight(15);
		staticText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
		staticText.setStyle(boldStyle);
		staticText.setText("Count : ");
		band.addElement(staticText);
		textField = new JRDesignTextField();
		textField.setX(460);
		textField.setY(0);
		textField.setWidth(30);
		textField.setHeight(15);
		textField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
		textField.setStyle(boldStyle);
		textField.setExpression(new JRDesignExpression("$V{CityGroup_COUNT}"));
		band.addElement(textField);
		((JRDesignSection)group.getGroupFooterSection()).addBand(band);

		klexDesign.addGroup(group);

		//Title
		band = new JRDesignBand();
		band.setHeight(50);
		line = new JRDesignLine();
		line.setX(0);
		line.setY(0);
		line.setWidth(515);
		line.setHeight(0);
		band.addElement(line);
		textField = new JRDesignTextField();
		textField.setBlankWhenNull(true);
		textField.setX(0);
		textField.setY(10);
		textField.setWidth(515);
		textField.setHeight(30);
		textField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
		textField.setStyle(normalStyle);
		textField.setFontSize(22f);
		textField.setExpression(new JRDesignExpression("$P{ReportTitle}"));
		band.addElement(textField);
		klexDesign.setTitle(band);
		
		//Page header
		band = new JRDesignBand();
		band.setHeight(20);
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(5);
		frame.setWidth(515);
		frame.setHeight(15);
		frame.setForecolor(new Color(0x33, 0x33, 0x33));
		frame.setBackcolor(new Color(0x33, 0x33, 0x33));
		frame.setMode(ModeEnum.OPAQUE);
		band.addElement(frame);
		staticText = new JRDesignStaticText();
		staticText.setX(0);
		staticText.setY(0);
		staticText.setWidth(55);
		staticText.setHeight(15);
		staticText.setForecolor(Color.white);
		staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
		staticText.setMode(ModeEnum.OPAQUE);
		staticText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
		staticText.setStyle(boldStyle);
		staticText.setText("ID");
		frame.addElement(staticText);
		staticText = new JRDesignStaticText();
		staticText.setX(55);
		staticText.setY(0);
		staticText.setWidth(205);
		staticText.setHeight(15);
		staticText.setForecolor(Color.white);
		staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
		staticText.setMode(ModeEnum.OPAQUE);
		staticText.setStyle(boldStyle);
		staticText.setText("Name");
		frame.addElement(staticText);
		staticText = new JRDesignStaticText();
		staticText.setX(260);
		staticText.setY(0);
		staticText.setWidth(255);
		staticText.setHeight(15);
		staticText.setForecolor(Color.white);
		staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
		staticText.setMode(ModeEnum.OPAQUE);
		staticText.setStyle(boldStyle);
		staticText.setText("Street");
		frame.addElement(staticText);
		klexDesign.setPageHeader(band);

		//Column header
		band = new JRDesignBand();
		klexDesign.setColumnHeader(band);

		//Detail
		band = new JRDesignBand();
		band.setHeight(20);
		textField = new JRDesignTextField();
		textField.setX(0);
		textField.setY(4);
		textField.setWidth(50);
		textField.setHeight(15);
		textField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
		textField.setStyle(normalStyle);
		textField.setExpression(new JRDesignExpression("$F{Id}"));
		band.addElement(textField);
		textField = new JRDesignTextField();
		textField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
		textField.setX(55);
		textField.setY(4);
		textField.setWidth(200);
		textField.setHeight(15);
		textField.setPositionType(PositionTypeEnum.FLOAT);
		textField.setStyle(normalStyle);
		textField.setExpression(new JRDesignExpression("$F{FirstName} + \" \" + $F{LastName}"));
		band.addElement(textField);
		textField = new JRDesignTextField();
		textField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
		textField.setX(260);
		textField.setY(4);
		textField.setWidth(255);
		textField.setHeight(15);
		textField.setPositionType(PositionTypeEnum.FLOAT);
		textField.setStyle(normalStyle);
		textField.setExpression(new JRDesignExpression("$F{Street}"));
		band.addElement(textField);
		line = new JRDesignLine();
		line.setX(0);
		line.setY(19);
		line.setWidth(515);
		line.setHeight(0);
		line.setForecolor(new Color(0x80, 0x80, 0x80));
		line.setPositionType(PositionTypeEnum.FLOAT);
		band.addElement(line);
		((JRDesignSection)klexDesign.getDetailSection()).addBand(band);

		//Column footer
		band = new JRDesignBand();
		klexDesign.setColumnFooter(band);

		//Page footer
		band = new JRDesignBand();
		klexDesign.setPageFooter(band);

		//Summary
		band = new JRDesignBand();
		klexDesign.setSummary(band);
		
		return klexDesign;
	}
	

}
