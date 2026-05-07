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
package net.sf.klexreports.virtualization;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.sf.klexreports.OwnVirtualizerContainer;
import net.sf.klexreports.PrintSerializer;
import net.sf.klexreports.Report;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.fill.JRGzipVirtualizer;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class VirtualizedFramesParentTest
{
	
	private Report report;

	@BeforeClass
	public void initReport() throws JRException, IOException
	{
		report = new Report("net/sf/klexreports/virtualization/repo/VirtualizedFramesParent.jrxml", 
				"net/sf/klexreports/virtualization/VirtualizedFramesParent.reference.jrpxml");
		report.addPrintConsumer(PrintSerializer.instance());
		report.addPrintConsumer(new PrintSerializer(new OwnVirtualizerContainer(new JRGzipVirtualizer(5))));
		report.init();
	}
	
	@Test
	public void virtualizedReport() throws JRException, NoSuchAlgorithmException, IOException
	{
		HashMap<String, Object> params = new HashMap<>();
		JRGzipVirtualizer virtualizer = new JRGzipVirtualizer(5);
		params.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
		params.put("VirtualizedFramesSubreport", 
				new Report("net/sf/klexreports/virtualization/repo/VirtualizedFrames").compileReport());
		
		report.runReport(params);
	}
}
