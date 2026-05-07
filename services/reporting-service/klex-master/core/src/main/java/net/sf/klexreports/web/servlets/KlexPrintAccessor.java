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
package net.sf.klexreports.web.servlets;

import net.sf.klexreports.engine.KlexPrint;

/**
 * {@link KlexPrint} accessor object.
 * 
 * Such an object is usually placed on the session when a report is generated.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface KlexPrintAccessor
{
	
	public static final String REPORT_CONTEXT_PARAMETER_KLEX_PRINT_ACCESSOR = "net.sf.klexreports.web.klex_print.accessor";

	/**
	 * Ensures that a page is available in the generated report.
	 * 
	 * @param pageIdx the page index
	 * @param pageTimestamp 
	 * @return the status of the requested page
	 */
	ReportPageStatus pageStatus(int pageIdx, Long pageTimestamp);
	
	/**
	 * Returns the generated report.
	 * 
	 * @return the generated report
	 */
	KlexPrint getKlexPrint();
	
	/**
	 * Returns the generated report, ensuring before that the report generation has ended.
	 * 
	 * @return the final generated report
	 */
	KlexPrint getFinalKlexPrint();

	/**
	 * Returns the status of the report execution.
	 * 
	 * @return the status of the report execution
	 */
	ReportExecutionStatus getReportStatus();

}
