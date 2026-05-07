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

public class HorizontalListOverflowTest
{
	
	@Test
	public void noOverflow() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/HorizontalListOverflow.jrxml", 
				"net/sf/klexreports/components/repo/HorizontalListOverflow-6.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		params.put("ItemCount", 6);
		report.runReport(params);
	}
	
	@Test
	public void oneRecordOverflow() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/HorizontalListOverflow.jrxml", 
				"net/sf/klexreports/components/repo/HorizontalListOverflow-7.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		params.put("ItemCount", 7);
		report.runReport(params);
	}
	
	@Test
	public void twoRecordsOverflow() throws JRException, IOException
	{
		Report report = new Report("net/sf/klexreports/components/repo/HorizontalListOverflow.jrxml", 
				"net/sf/klexreports/components/repo/HorizontalListOverflow-8.jrpxml");
		report.init();
		
		Map<String, Object> params = new HashMap<>();
		params.put("ItemCount", 8);
		report.runReport(params);
	}

}
