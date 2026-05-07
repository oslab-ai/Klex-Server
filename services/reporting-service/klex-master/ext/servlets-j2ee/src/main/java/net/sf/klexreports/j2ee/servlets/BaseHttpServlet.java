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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class BaseHttpServlet extends HttpServlet
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	
	/**
	 *
	 */
	public static final String DEFAULT_KLEX_PRINT_LIST_SESSION_ATTRIBUTE = "net.sf.klexreports.j2ee.klex_print_list";
	public static final String DEFAULT_KLEX_PRINT_SESSION_ATTRIBUTE = "net.sf.klexreports.j2ee.klex_print";

	public static final String KLEX_PRINT_LIST_REQUEST_PARAMETER = "jrprintlist";
	public static final String KLEX_PRINT_REQUEST_PARAMETER = "jrprint";

	public static final String BUFFERED_OUTPUT_REQUEST_PARAMETER = "buffered"; 
	
			
	/**
	 *
	 */
	public KlexReportsContext getKlexReportsContext()
	{
		return DefaultKlexReportsContext.getInstance();
	}

	/**
	 *
	 */
	public static List<KlexPrint> getKlexPrintList(HttpServletRequest request)
	{
		String klexPrintListSessionAttr = request.getParameter(KLEX_PRINT_LIST_REQUEST_PARAMETER);
		if (klexPrintListSessionAttr == null)
		{
			klexPrintListSessionAttr = DEFAULT_KLEX_PRINT_LIST_SESSION_ATTRIBUTE;
		}

		String klexPrintSessionAttr = request.getParameter(KLEX_PRINT_REQUEST_PARAMETER);
		if (klexPrintSessionAttr == null)
		{
			klexPrintSessionAttr = DEFAULT_KLEX_PRINT_SESSION_ATTRIBUTE;
		}
		
		List<KlexPrint> klexPrintList = (List<KlexPrint>)request.getSession().getAttribute(klexPrintListSessionAttr);
		if (klexPrintList == null)
		{
			KlexPrint klexPrint = (KlexPrint)request.getSession().getAttribute(klexPrintSessionAttr);
			if (klexPrint != null)
			{
				klexPrintList = new ArrayList<>();
				klexPrintList.add(klexPrint);
			}
		}
		
		return klexPrintList;
	}


}
