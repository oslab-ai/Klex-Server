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
package net.sf.klexreports.async;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.fill.FillListener;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class GroupKeepTogetherPageUpdateTest
{
	
	private AsyncReport report;

	@BeforeClass
	public void initReport() throws JRException, IOException
	{
		report = new AsyncReport("net/sf/klexreports/async/repo/GroupKeepTogether.jrxml", 
				"net/sf/klexreports/async/repo/GroupKeepTogether.reference.jrpxml");
		report.init();
	}
	
	@Test
	public void report() throws JRException, NoSuchAlgorithmException, IOException
	{
		final AtomicInteger secondPageUpdateCount = new AtomicInteger(0);
		report.runReport(null, new FillListener()
		{
			@Override
			public void pageUpdated(KlexPrint klexPrint, int pageIndex)
			{
				if (pageIndex == 1)
				{
					secondPageUpdateCount.incrementAndGet();
				}
			}
			
			@Override
			public void pageGenerated(KlexPrint klexPrint, int pageIndex)
			{
				//NOP
			}
		});
		
		assert secondPageUpdateCount.get() == 1;
	}

}
