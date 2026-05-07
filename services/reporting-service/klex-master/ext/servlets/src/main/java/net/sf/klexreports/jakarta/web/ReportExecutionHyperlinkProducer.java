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
package net.sf.klexreports.jakarta.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import net.sf.klexreports.engine.JRPrintHyperlink;
import net.sf.klexreports.engine.JRPrintHyperlinkParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.JRHyperlinkProducer;
import net.sf.klexreports.web.util.ResourcePathUtil;
import net.sf.klexreports.web.util.WebConstants;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ReportExecutionHyperlinkProducer implements JRHyperlinkProducer
{
	public static final String HYPERLINK_TYPE_REPORT_EXECUTION = "ReportExecution";
	public static final String PARAMETER_REPORT_URI = "jr.report";
	private static final String PARAMETER_REPORT_URI_OLD = "jr.uri";
	//private static final String PARAMETER_REPORT_URI_OLD = "_report";
	
	protected KlexReportsContext klexReportsContext;
	private HttpServletRequest request;
	
	/**
	 *
	 */
	protected ReportExecutionHyperlinkProducer(KlexReportsContext klexReportsContext,HttpServletRequest request)
	{
		this.klexReportsContext = klexReportsContext;
		this.request = request;
	}

	/**
	 *
	 */
	public static ReportExecutionHyperlinkProducer getInstance(KlexReportsContext klexReportsContext, HttpServletRequest request)
	{
		return new ReportExecutionHyperlinkProducer(klexReportsContext, request);
	}


	/**
	 *
	 */
	protected String getPath() 
	{
		return ResourcePathUtil.getInstance(klexReportsContext).getReportExecutionPath();
	}
	
	
	@Override
	public String getHyperlink(JRPrintHyperlink hyperlink) 
	{
		String applicationDomain = null;
		String servletPath = getPath();
		String reportUri = request.getParameter(WebConstants.REQUEST_PARAMETER_REPORT_URI);

		WebReportContext webReportContext = WebReportContext.getInstance(request, false);
		if (webReportContext != null) {
			applicationDomain = (String) webReportContext.getParameterValue(WebReportContext.REQUEST_PARAMETER_APPLICATION_DOMAIN);
		}

		if (applicationDomain == null) {
			applicationDomain = request.getContextPath();
		}
		
		StringBuilder allParams = new StringBuilder();
		
		if (hyperlink.getHyperlinkParameters() != null)
		{
			List<JRPrintHyperlinkParameter> parameters = hyperlink.getHyperlinkParameters().getParameters();
			if (parameters != null)
			{
				for (int i = 0; i < parameters.size(); i++)
				{
					JRPrintHyperlinkParameter parameter = parameters.get(i);
					if (
						PARAMETER_REPORT_URI.equals(parameter.getName())
						|| PARAMETER_REPORT_URI_OLD.equals(parameter.getName())
						)
					{
						reportUri = (String)parameter.getValue();
					}
//					else if (FillServlet.REPORT_ACTION.equals(parameter.getName()))
//					{
//						reportAction = (String)parameter.getValue();
//					}
//					else if (FillServlet.REPORT_ACTION_DATA.equals(parameter.getName()))
//					{
//						reportActionData = (String)parameter.getValue();
//					}
					else if (parameter.getValue() != null)
					{
						allParams.append("&").append(parameter.getName()).append("=").append(parameter.getValue());
					}
				}
			}
		}
		
		return
				applicationDomain + (servletPath != null ? servletPath : "")
				+ "?" + WebConstants.REQUEST_PARAMETER_REPORT_URI + "=" + reportUri
//				+ (reportAction == null ? "" : "&" + FillServlet.REPORT_ACTION + "=" + reportAction) 
//				+ (reportActionData == null ? "" : "&" + FillServlet.REPORT_ACTION_DATA + "=" + reportActionData)
				+ allParams.toString();
	}

}
