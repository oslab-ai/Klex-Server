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

import java.io.Serializable;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexPrint;

/**
 * Generated report accessor used for fully generated reports.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SimpleKlexPrintAccessor implements KlexPrintAccessor, Serializable
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private final KlexPrint klexPrint;
	
	/**
	 * Create a report accessor.
	 * 
	 * @param klexPrint the generated report
	 */
	public SimpleKlexPrintAccessor(KlexPrint klexPrint)
	{
		this.klexPrint = klexPrint;
	}

	@Override
	public ReportPageStatus pageStatus(int pageIdx, Long pageTimestamp)
	{
		return pageIdx < klexPrint.getPages().size() ? ReportPageStatus.PAGE_FINAL : ReportPageStatus.NO_SUCH_PAGE;
	}

	@Override
	public KlexPrint getKlexPrint()
	{
		return klexPrint;
	}

	@Override
	public KlexPrint getFinalKlexPrint()
	{
		return klexPrint;
	}

	@Override
	public ReportExecutionStatus getReportStatus()
	{
		return ReportExecutionStatus.finished(klexPrint.getPages().size());
	}

}
