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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexPrintManager;
import net.sf.klexreports.engine.export.JRCsvExporter;
import net.sf.klexreports.engine.export.JRRtfExporter;
import net.sf.klexreports.engine.export.oasis.JROdsExporter;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.export.ooxml.JRDocxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.query.HibernateConstants;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleOdsReportConfiguration;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleWriterExporterOutput;
import net.sf.klexreports.export.SimpleXlsReportConfiguration;
import net.sf.klexreports.export.SimpleXlsxReportConfiguration;
import net.sf.klexreports.poi.export.JRXlsExporter;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class HibernateApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new HibernateApp(), args);
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
	public void fill() throws JRException
	{
		SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();

		Map<String, Object> params = getParameters(session);
		
		File[] files = 
			new File[]{
				new File("target/reports/AddressesReport.klex"),
				new File("target/reports/HibernateQueryReport.klex")
			};
		for(int i = 0; i < files.length; i++)
		{
			File reportFile = files[i];
			long start = System.currentTimeMillis();
			KlexFillManager.fillReportToFile(reportFile.getAbsolutePath(), new HashMap<String, Object>(params));
			System.err.println("Report : " + reportFile + ". Filling time : " + (System.currentTimeMillis() - start));
		}
		
		transaction.rollback();
		sessionFactory.close();
	}
	
	
	/**
	 *
	 */
	public void print() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			File reportFile = files[i];
			long start = System.currentTimeMillis();
			KlexPrintManager.printReport(reportFile.getAbsolutePath(), true);
			System.err.println("Report : " + reportFile + ". Printing time : " + (System.currentTimeMillis() - start));
		}
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
			KlexExportManager.exportReportToPdfFile(reportFile.getAbsolutePath());
			System.err.println("Report : " + reportFile + ". PDF creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void rtf() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];

			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".rtf");
		
			JRRtfExporter exporter = new JRRtfExporter();
		
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". RTF creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void xml() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			File reportFile = files[i];
			long start = System.currentTimeMillis();
			KlexExportManager.exportReportToXmlFile(reportFile.getAbsolutePath(), false);
			System.err.println("Report : " + reportFile + ". XML creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void xmlEmbed() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			File reportFile = files[i];
			long start = System.currentTimeMillis();
			KlexExportManager.exportReportToXmlFile(reportFile.getAbsolutePath(), true);
			System.err.println("Report : " + reportFile + ". XML creation time : " + (System.currentTimeMillis() - start));
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
			KlexExportManager.exportReportToHtmlFile(reportFile.getAbsolutePath());
			System.err.println("Report : " + reportFile + ". HTML creation time : " + (System.currentTimeMillis() - start));
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
			exporter.setConfiguration(configuration);
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". XLS creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void csv() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];

			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".csv");
		
			JRCsvExporter exporter = new JRCsvExporter();
		
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". CSV creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void odt() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];

			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".odt");
		
			JROdtExporter exporter = new JROdtExporter();
		
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". ODT creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void ods() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];

			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".ods");
		
			JROdsExporter exporter = new JROdsExporter();
		
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
			SimpleOdsReportConfiguration configuration = new SimpleOdsReportConfiguration();
			configuration.setOnePagePerSheet(true);
			exporter.setConfiguration(configuration);
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". ODS creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void docx() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];

			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".docx");
		
			JRDocxExporter exporter = new JRDocxExporter();
		
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". DOCX creation time : " + (System.currentTimeMillis() - start));
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

			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".xlsx");
		
			JRXlsxExporter exporter = new JRXlsxExporter();
		
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
			SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
			configuration.setOnePagePerSheet(true);
			exporter.setConfiguration(configuration);
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". XLSX creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	/**
	 *
	 */
	public void pptx() throws JRException
	{
		File[] files = getFiles(new File("target/reports"), "jrprint");
		for(int i = 0; i < files.length; i++)
		{
			long start = System.currentTimeMillis();
			File sourceFile = files[i];

			KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

			File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".pptx");
		
			JRPptxExporter exporter = new JRPptxExporter();
		
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		
			exporter.exportReport();

			System.err.println("Report : " + sourceFile + ". PPTX creation time : " + (System.currentTimeMillis() - start));
		}
	}
	
	
	private static Map<String, Object> getParameters(Session session)
	{
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(HibernateConstants.PARAMETER_HIBERNATE_SESSION, session);
		parameters.put("ReportTitle", "Address Report");
		List<String> cityFilter = new ArrayList<String>(3);
		cityFilter.add("Boston");
		cityFilter.add("Chicago");
		cityFilter.add("Oslo");
		parameters.put("CityFilter", cityFilter);
		parameters.put("OrderClause", "city");
		return parameters;
	}

}
