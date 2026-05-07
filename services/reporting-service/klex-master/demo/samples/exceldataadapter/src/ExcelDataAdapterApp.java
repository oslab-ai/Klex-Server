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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.klexreports.dataadapters.DataAdapterParameterContributorFactory;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class ExcelDataAdapterApp extends AbstractSampleApp
{
	

	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new ExcelDataAdapterApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		pdf();
		html();
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
		Set<String> states = new HashSet<String>();
		states.add("Active");
		states.add("Trial");
		parameters.put("IncludedStates", states);

		//query executer mode
		parameters.put("DataFile", "XLS query executer mode for Excel data adapter");
		KlexFillManager.fillReportToFile("target/reports/ExcelXlsQeDataAdapterReport.klex", new HashMap<String, Object>(parameters));
		parameters.put("DataFile", "XLSX query executer mode for Excel data adapter");
		KlexFillManager.fillReportToFile("target/reports/ExcelXlsxQeDataAdapterReport.klex", new HashMap<String, Object>(parameters));
		
		KlexReport klexReport = (KlexReport)JRLoader.loadObjectFromFile("target/reports/ExcelXlsQeDataAdapterReport.klex");
		klexReport.setProperty(DataAdapterParameterContributorFactory.PROPERTY_DATA_ADAPTER_LOCATION, "data/XlsQeDataAdapter.jrdax");
		KlexFillManager.fillReportToFile(klexReport, "target/reports/XlsQeDataAdapterReport.jrprint", new HashMap<String, Object>(parameters));

		klexReport = (KlexReport)JRLoader.loadObjectFromFile("target/reports/ExcelXlsxQeDataAdapterReport.klex");
		klexReport.setProperty(DataAdapterParameterContributorFactory.PROPERTY_DATA_ADAPTER_LOCATION, "data/XlsxQeDataAdapter.jrdax");
		KlexFillManager.fillReportToFile(klexReport, "target/reports/XlsxQeDataAdapterReport.jrprint", new HashMap<String, Object>(parameters));
		
		//data source mode
		parameters.put("DataFile", "Excel data adapter for XLS data source");
		KlexFillManager.fillReportToFile("target/reports/ExcelXlsDataAdapterReport.klex", new HashMap<String, Object>(parameters));
		parameters.put("DataFile", "Excel data adapter for XLSX data source");
		KlexFillManager.fillReportToFile("target/reports/ExcelXlsxDataAdapterReport.klex", new HashMap<String, Object>(parameters));
		
		klexReport = (KlexReport)JRLoader.loadObjectFromFile("target/reports/ExcelXlsDataAdapterReport.klex");
		klexReport.setProperty(DataAdapterParameterContributorFactory.PROPERTY_DATA_ADAPTER_LOCATION, "data/XlsDataAdapter.jrdax");
		KlexFillManager.fillReportToFile(klexReport, "target/reports/XlsDataAdapterReport.jrprint", new HashMap<String, Object>(parameters));

		klexReport = (KlexReport)JRLoader.loadObjectFromFile("target/reports/ExcelXlsxDataAdapterReport.klex");
		klexReport.setProperty(DataAdapterParameterContributorFactory.PROPERTY_DATA_ADAPTER_LOCATION, "data/XlsxDataAdapter.jrdax");
		KlexFillManager.fillReportToFile(klexReport, "target/reports/XlsxDataAdapterReport.jrprint", new HashMap<String, Object>(parameters));
		
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void print() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexPrintManager.printReport("target/reports/ExcelXlsDataAdapterReport.jrprint", true);
		System.err.println("Printing time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void pdf() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			File reportFile = files[i];
			long start = System.currentTimeMillis();
			String fileName = reportFile.getAbsolutePath();
			KlexExportManager.exportReportToPdfFile(
				fileName, 
				fileName.substring(0, fileName.indexOf(".jrprint")) + ".pdf"
				);
			System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
		}
	}


	/**
	 *
	 */
	public void html() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			File reportFile = files[i];
			long start = System.currentTimeMillis();
			String fileName = reportFile.getAbsolutePath();
			KlexExportManager.exportReportToHtmlFile(
					fileName, 
					fileName.substring(0, fileName.indexOf(".jrprint")) + ".html"
				);
			System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
		}
		
	}


}
