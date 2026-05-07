<%--
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
--%>

<%@ page errorPage="error.jsp" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>

<%@ page import="datasource.WebappDataSource" %>
<%@ page import="net.sf.klexreports.engine.JRRuntimeException" %>
<%@ page import="net.sf.klexreports.engine.KlexFillManager" %>
<%@ page import="net.sf.klexreports.engine.KlexPrint" %>
<%@ page import="net.sf.klexreports.engine.KlexReport" %>
<%@ page import="net.sf.klexreports.engine.export.HtmlExporter" %>
<%@ page import="net.sf.klexreports.engine.util.JRLoader" %>
<%@ page import="net.sf.klexreports.export.SimpleExporterInput" %>
<%@ page import="net.sf.klexreports.export.SimpleHtmlExporterOutput" %>
<%@ page import="net.sf.klexreports.jakarta.servlets.ImageServlet" %>
<%@ page import="net.sf.klexreports.web.util.WebHtmlResourceHandler" %>


<%
	File reportFile = new File(application.getRealPath("/reports/WebappReport.klex"));
    if (!reportFile.exists())
		throw new JRRuntimeException("File WebappReport.klex not found. The report design must be compiled first.");

	KlexReport klexReport = (KlexReport)JRLoader.loadObjectFromFile(reportFile.getPath());

	Map parameters = new HashMap();
	parameters.put("ReportTitle", "Address Report");
	parameters.put("BaseDir", reportFile.getParentFile());
				
	KlexPrint klexPrint = 
		KlexFillManager.fillReport(
			klexReport, 
			parameters, 
			new WebappDataSource()
			);
				
	HtmlExporter exporter = new HtmlExporter();

	session.setAttribute(ImageServlet.DEFAULT_KLEX_PRINT_SESSION_ATTRIBUTE, klexPrint);
	
	exporter.setExporterInput(new SimpleExporterInput(klexPrint));
	SimpleHtmlExporterOutput output = new SimpleHtmlExporterOutput(out);
	output.setImageHandler(new WebHtmlResourceHandler("../servlets/image?image={0}"));
	exporter.setExporterOutput(output);
	
	exporter.exportReport();
%>

