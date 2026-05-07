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
package net.sf.klexreports.jakarta.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.export.JRXmlExporter;
import net.sf.klexreports.engine.util.FileBufferedOutputStream;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleReportExportConfiguration;
import net.sf.klexreports.export.SimpleXmlExporterOutput;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class XmlServlet extends BaseHttpServlet
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */
	public static final String START_PAGE_INDEX_REQUEST_PARAMETER = "startPage";
	public static final String END_PAGE_INDEX_REQUEST_PARAMETER = "endPage";
	public static final String PAGE_INDEX_REQUEST_PARAMETER = "page";
	

	@Override
	public void service(
		HttpServletRequest request,
		HttpServletResponse response
		) throws IOException, ServletException
	{
		List<KlexPrint> klexPrintList = BaseHttpServlet.getKlexPrintList(request);

		if (klexPrintList == null)
		{
			throw new ServletException("No KlexPrint documents found on the HTTP session.");
		}
		
		int startPageIndex = -1;

		String startPageStr = request.getParameter(START_PAGE_INDEX_REQUEST_PARAMETER);
		try
		{
			startPageIndex = Integer.parseInt(startPageStr);
		}
		catch(Exception e)
		{
		}
		
		int endPageIndex = -1;

		String endPageStr = request.getParameter(END_PAGE_INDEX_REQUEST_PARAMETER);
		try
		{
			endPageIndex = Integer.parseInt(endPageStr);
		}
		catch(Exception e)
		{
		}
		
		int pageIndex = -1;

		String pageStr = request.getParameter(PAGE_INDEX_REQUEST_PARAMETER);
		try
		{
			pageIndex = Integer.parseInt(pageStr);
		}
		catch(Exception e)
		{
		}
		
		if (pageIndex >= 0)
		{
			startPageIndex = pageIndex;
			endPageIndex = pageIndex;
		}
		
		Boolean isBuffered = Boolean.valueOf(request.getParameter(BaseHttpServlet.BUFFERED_OUTPUT_REQUEST_PARAMETER));
		if (isBuffered)
		{
			FileBufferedOutputStream fbos = new FileBufferedOutputStream();
			JRXmlExporter exporter = getExporter();
			exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
			SimpleReportExportConfiguration configuration = new SimpleReportExportConfiguration();
			if (startPageIndex >= 0)
			{
				configuration.setStartPageIndex(startPageIndex);
			}
			if (endPageIndex >= 0)
			{
				configuration.setEndPageIndex(endPageIndex);
			}
			exporter.setExporterOutput(new SimpleXmlExporterOutput(fbos));

			try 
			{
				exporter.exportReport();
				fbos.close();
			
				if (fbos.size() > 0)
				{
					response.setContentType("text/xml");
					response.setHeader("Content-Disposition", "inline; filename=\"file.jrpxml\"");
					response.setContentLength(fbos.size());
					ServletOutputStream outputStream = response.getOutputStream();
	
					try
					{
						fbos.writeData(outputStream);
						fbos.dispose();
						outputStream.flush();
					}
					finally
					{
						if (outputStream != null)
						{
							try
							{
								outputStream.close();
							}
							catch (IOException ex)
							{
							}
						}
					}
				}
			} 
			catch (JRException e) 
			{
				throw new ServletException(e);
			}
			finally
			{
				fbos.close();
				fbos.dispose();
			}
//			else
//			{
//				response.setContentType("text/html");
//				PrintWriter out = response.getWriter();
//				out.println("<html>");
//				out.println("<body bgcolor=\"white\">");
//				out.println("<span class=\"bold\">Empty response.</span>");
//				out.println("</body>");
//				out.println("</html>");
//			}
		}
		else
		{
			response.setContentType("text/xml");
			response.setHeader("Content-Disposition", "inline; filename=\"file.jrpxml\"");

			JRXmlExporter exporter = getExporter();
			exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
			SimpleReportExportConfiguration configuration = new SimpleReportExportConfiguration();
			if (startPageIndex >= 0)
			{
				configuration.setStartPageIndex(startPageIndex);
			}
			if (endPageIndex >= 0)
			{
				configuration.setEndPageIndex(endPageIndex);
			}
			
			OutputStream outputStream = response.getOutputStream();
			exporter.setExporterOutput(new SimpleXmlExporterOutput(outputStream));

			try 
			{
				exporter.exportReport();
			} 
			catch (JRException e) 
			{
				throw new ServletException(e);
			}
			finally
			{
				if (outputStream != null)
				{
					try
					{
						outputStream.close();
					}
					catch (IOException ex)
					{
					}
				}
			}
		}
	}

	
	/**
	 * 
	 */
	public JRXmlExporter getExporter()
	{
		return new JRXmlExporter(DefaultKlexReportsContext.getInstance());
	}
}

