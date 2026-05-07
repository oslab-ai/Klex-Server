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
package net.sf.klexreports.j2ee.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.util.FileBufferedOutputStream;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class OdtServlet extends BaseHttpServlet
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;


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
		
		Boolean isBuffered = Boolean.valueOf(request.getParameter(BaseHttpServlet.BUFFERED_OUTPUT_REQUEST_PARAMETER));
		if (isBuffered)
		{
			FileBufferedOutputStream fbos = new FileBufferedOutputStream();
			JROdtExporter exporter = new JROdtExporter(DefaultKlexReportsContext.getInstance());
			exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(fbos));

			try 
			{
				exporter.exportReport();
				fbos.close();
			
				if (fbos.size() > 0)
				{
					response.setContentType("application/vnd.oasis.opendocument.text");
	//				response.setHeader("Content-Disposition", "inline; filename=\"file.odt\"");
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
			response.setContentType("application/vnd.oasis.opendocument.text");
			//response.setHeader("Content-Disposition", "inline; filename=\"file.odt\"");

			JROdtExporter exporter = new JROdtExporter(DefaultKlexReportsContext.getInstance());
			exporter.setExporterInput(SimpleExporterInput.getInstance(klexPrintList));
			
			OutputStream outputStream = response.getOutputStream();
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

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

	
}

