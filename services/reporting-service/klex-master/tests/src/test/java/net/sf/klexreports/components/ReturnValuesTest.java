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
package net.sf.klexreports.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import net.sf.klexreports.Report;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReport;

public class ReturnValuesTest
{
	
	@Test
	public void tableReturn() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/TableReturn.jrxml", 
				"net/sf/klexreports/components/repo/TableReturn.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		report.runReport(params);
	}
	
	@Test
	public void tableWithSubreportReturn() throws JRException, IOException
	{
		Report subreport = new Report("net/sf/klexreports/components/repo/SubreportForReturn.jrxml", null);
		KlexReport compiledSubreport = subreport.compileReport();
		
		Report report = new Report("net/sf/klexreports/components/repo/TableWithSubreportReturn.jrxml", 
				"net/sf/klexreports/components/repo/TableWithSubreportReturn.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		params.put("subreport", compiledSubreport);
		report.runReport(params);
	}
	
	@Test
	public void tableWithListReturn() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/TableWithListReturn.jrxml", 
				"net/sf/klexreports/components/repo/TableWithListReturn.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		report.runReport(params);
	}
	
	@Test
	public void tableWithTableReturn() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/TableWithTableReturn.jrxml", 
				"net/sf/klexreports/components/repo/TableWithTableReturn.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		report.runReport(params);
	}
	
	@Test
	public void listWithSubreportReturn() throws JRException, IOException
	{
		Report subreport = new Report("net/sf/klexreports/components/repo/SubreportForReturn.jrxml", null);
		KlexReport compiledSubreport = subreport.compileReport();
		
		Report report = new Report("net/sf/klexreports/components/repo/ListWithSubreportReturn.jrxml", 
				"net/sf/klexreports/components/repo/ListWithSubreportReturn.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		params.put("subreport", compiledSubreport);
		report.runReport(params);
	}
	
	@Test
	public void listWithListReturn() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/ListWithListReturn.jrxml", 
				"net/sf/klexreports/components/repo/ListWithListReturn.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		report.runReport(params);
	}
	
	@Test
	public void listWithTableReturn() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/ListWithTableReturn.jrxml", 
				"net/sf/klexreports/components/repo/ListWithTableReturn.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		report.runReport(params);
	}

}
