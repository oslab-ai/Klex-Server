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

import java.util.Map;

import net.sf.klexreports.Report;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.fill.AsynchronousFillHandle;
import net.sf.klexreports.engine.fill.FillListener;
import net.sf.klexreports.web.servlets.AsyncKlexPrintAccessor;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class AsyncReport extends Report
{

	public AsyncReport(String jrxml, String jrpxml)
	{
		super(jrxml, jrpxml);
	}
	
	public void runReport(Map<String, Object> params, FillListener fillListener)
	{
		try
		{
			Map<String, Object> reportParams = reportParams(params);
			AsynchronousFillHandle asyncHandle = AsynchronousFillHandle.createHandle(
					klexReportsContext, report, reportParams);
			
			if (fillListener != null)
			{
				asyncHandle.addFillListener(fillListener);
			}
			
			AsyncKlexPrintAccessor accessor = new AsyncKlexPrintAccessor(asyncHandle);
			asyncHandle.startFill();
			KlexPrint print = accessor.getFinalKlexPrint();
			reportComplete(reportParams, print);
		}
		catch (JRException e)
		{
			throw new RuntimeException(e);
		}
	}

}
