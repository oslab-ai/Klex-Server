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

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.util.AbstractSampleApp;


/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JsonQLDataSourceApp extends AbstractSampleApp
{
	

	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new JsonQLDataSourceApp(), args);
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
//		Map<String, Object> parameters = new HashMap<>();
//		parameters.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
//		parameters.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
//		parameters.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
//		parameters.put(JRParameter.REPORT_LOCALE, Locale.US);

		File[] files = getFiles(new File("target/reports"), "klex");
		for(int i = 0; i < files.length; i++)
		{
			File reportFile = files[i];
			long start = System.currentTimeMillis();
			KlexFillManager.fillReportToFile(reportFile.getAbsolutePath(), new HashMap<String, Object>());
			System.err.println("Report : " + reportFile + ". Filling time : " + (System.currentTimeMillis() - start));
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
			System.err.println("Report : " + reportFile + ". PDF export time : " + (System.currentTimeMillis() - start));
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
			System.err.println("Report : " + reportFile + ". HTML export time : " + (System.currentTimeMillis() - start));
		}
	}

}
