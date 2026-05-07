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

/*
 * Contributors:
 * Greg Hilton 
 */

package net.sf.klexreports.engine.export;

import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPrintFrame;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.KlexReportsContext;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRCsvExporterNature extends AbstractExporterNature
{
	
	/**
	 * 
	 */
	public JRCsvExporterNature(KlexReportsContext klexReportsContext, ExporterFilter filter)
	{
		super(klexReportsContext, filter);
	}
	
	@Override
	public boolean isToExport(JRPrintElement element)
	{
//		JRPrintFrame frame = element instanceof JRPrintFrame ? (JRPrintFrame)element : null;
//		if (frame != null)
//		{
//			List<JRPrintElement> elements = frame.getElements();
//			return elements != null && elements.size() > 0;
//		}
//		return (element instanceof JRPrintText || element instanceof JRGenericPrintElement)
//			&& (filter == null || filter.isToExport(element));
		if (element instanceof JRGenericPrintElement)
		{
			JRGenericPrintElement genericElement = (JRGenericPrintElement) element;
			GenericElementHandler handler = handlerEnvironment.getElementHandler(
					genericElement.getGenericType(), JRAbstractCsvExporter.CSV_EXPORTER_KEY);
			if (handler == null || !handler.toExport(genericElement))
			{
				return false;
			}
		}
		
		return (element instanceof JRPrintText || element instanceof JRPrintFrame || element instanceof JRGenericPrintElement)
			&& (filter == null || filter.isToExport(element));
	}
	
	@Override
	public boolean isDeep(JRPrintFrame frame)
	{
		return true;
	}

	@Override
	public boolean isSpanCells()
	{
		return false;
	}
	
	@Override
	public boolean isIgnoreLastRow()
	{
		return false;
	}

	@Override
	public boolean isHorizontallyMergeEmptyCells()
	{
		return false;
	}

	/**
	 * Specifies whether empty page margins should be ignored
	 */
	@Override
	public boolean isIgnorePageMargins()
	{
		return false;
	}
	
	@Override
	public boolean isBreakBeforeRow(JRPrintElement element)
	{
		return false;
	}
	
	@Override
	public boolean isBreakAfterRow(JRPrintElement element)
	{
		return false;
	}
	
}
