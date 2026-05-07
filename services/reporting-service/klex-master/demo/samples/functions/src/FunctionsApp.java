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

import java.util.Collection;
import java.util.Locale;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexExportManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.data.JRBeanCollectionDataSource;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.functions.annotations.FunctionCategoryBean;
import net.sf.klexreports.functions.annotations.FunctionsInfo;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class FunctionsApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new FunctionsApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		html();
	}


	/**
	 *
	 */
	public void fill() throws JRException
	{
		long start = System.currentTimeMillis();
		JRDataSource datasource = createDataSource(DefaultKlexReportsContext.getInstance(), Locale.US);
		KlexFillManager.fillReportToFile("target/reports/FunctionsReport.klex", null, datasource);
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}
	
	
	/**
	 *
	 */
	public void html() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexExportManager.exportReportToHtmlFile("target/reports/FunctionsReport.jrprint");
		System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
	}
	
	/**
	 *
	 */
	public JRDataSource createDataSource(
		KlexReportsContext klexReportsContext, 
		Locale locale 
		) 
	{
		FunctionsInfo functionsInfo = FunctionsInfo.getInstance(klexReportsContext, locale);
		Collection<FunctionCategoryBean> categories = functionsInfo.getCategories(); 
		return new JRBeanCollectionDataSource(categories);
	}


}
