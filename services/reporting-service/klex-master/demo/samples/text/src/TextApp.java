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
import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.export.JRTextExporter;
import net.sf.klexreports.engine.export.JRXmlExporter;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.export.SimpleXmlExporterOutput;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class TextApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args) 
	{
		main(new TextApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		pdf();
		xml();
		text();
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
		parameters.put("FilterClause", "'Boston', 'Chicago', 'Oslo'");
		parameters.put("OrderClause", "City");

		KlexFillManager.fillReportToFile("target/reports/TextReport.klex", parameters, getDemoHsqldbConnection());
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void print() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexPrintManager.printReport("target/reports/TextReport.jrprint", true);
		System.err.println("Printing time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void text() throws JRException
	{
		long start = System.currentTimeMillis();
		JRTextExporter exporter = new JRTextExporter();
		File sourceFile = new File("target/reports/TextReport.jrprint");
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);
		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".txt");

		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));
		exporter.exportReport();

		System.err.println("Text creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void pdf() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToPdfFile("target/reports/TextReport.jrprint");
		System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void xml() throws JRException
	{
		long start = System.currentTimeMillis();
		File sourceFile = new File("target/reports/TextReport.jrprint");

		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".jrpxml");
		
		JRXmlExporter exporter = new JRXmlExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleXmlExporterOutput(destFile));
		
		exporter.exportReport();

		System.err.println("XML creation time : " + (System.currentTimeMillis() - start));
	}


}
