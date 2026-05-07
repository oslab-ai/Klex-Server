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
package net.sf.klexreports.j2ee.web;

import javax.servlet.http.HttpServletRequest;

import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.JRHyperlinkProducer;
import net.sf.klexreports.engine.export.JRHyperlinkProducerFactory;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ReportExecutionHyperlinkProducerFactory extends JRHyperlinkProducerFactory
{
	private KlexReportsContext klexReportsContext;
	private HttpServletRequest request;
	
	/**
	 *
	 */
	private ReportExecutionHyperlinkProducerFactory(KlexReportsContext klexReportsContext, HttpServletRequest request)
	{
		this.klexReportsContext = klexReportsContext;
		this.request = request;
	}

	/**
	 *
	 */
	public static ReportExecutionHyperlinkProducerFactory getInstance(KlexReportsContext klexReportsContext, HttpServletRequest request)
	{
		return new ReportExecutionHyperlinkProducerFactory(klexReportsContext, request);
	}

	@Override
	public JRHyperlinkProducer getHandler(String linkType)
	{
		if (linkType != null)
		{
			if (ReportExecutionHyperlinkProducer.HYPERLINK_TYPE_REPORT_EXECUTION.equals(linkType))
			{
				return ReportExecutionHyperlinkProducer.getInstance(klexReportsContext, request);
			}
		}
		return null;
	}

}
