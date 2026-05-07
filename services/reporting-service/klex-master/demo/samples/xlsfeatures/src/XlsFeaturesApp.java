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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleXlsReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxReportConfiguration;
import net.sf.klexreports.poi.export.JRXlsExporter;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class XlsFeaturesApp extends AbstractSampleApp
{
	

	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new XlsFeaturesApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		xls();
		xlsx();
	}


	/**
	 *
	 */
	public void fill() throws JRException
	{
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("ReportTitle", "Customers Report");
		parameters.put("Customers", "Customers");
		parameters.put("ReportDate", new Date());
		parameters.put("DataFile", "CsvDataSource.txt - CSV query executer");

		File[] files = getFiles(new File("target/reports"), "klex");
		for(int i = 0; i< files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];
			KlexFillManager.fillReportToFile(sourceFile.getPath(), new HashMap<String, Object>(parameters));
			System.err.println("Report : " + sourceFile + ". Filling time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void xls() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];
	
			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);
	
			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xls");
			
			JRXlsExporter exporter = new JRXlsExporter();
			
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
			SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
			configuration.setOnePagePerSheet(true);
			configuration.setDetectCellType(true);
			configuration.setCollapseRowSpan(false);
			exporter.setConfiguration(configuration);
			
			exporter.exportReport();
	
			System.err.println("Report : " + sourceFile + ". XLS creation time : " + (System.currentTimeMillis() - start));
	
		}
	}
	
	
	/**
	 *
	 */
	public void xlsx() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];
	
			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);
			String extension = klexPrint.getName().contains("Macro") ? ".xlsm" : ".xlsx";
			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + extension);
			
			JRXlsxExporter exporter = new JRXlsxExporter();
			
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
			SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
			configuration.setOnePagePerSheet(true);
			configuration.setDetectCellType(true);
			configuration.setCollapseRowSpan(false);
			exporter.setConfiguration(configuration);
			
			exporter.exportReport();
	
			System.err.println("Report : " + sourceFile + ". "+ extension.toUpperCase() + " creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
}
