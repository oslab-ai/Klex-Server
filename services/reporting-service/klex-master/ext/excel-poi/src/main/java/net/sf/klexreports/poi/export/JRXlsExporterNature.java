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

package net.sf.klexreports.poi.export;

import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.ExcelAbstractExporter;
import net.sf.klexreports.engine.export.ExporterFilter;
import net.sf.klexreports.engine.export.GenericElementHandler;
import net.sf.klexreports.engine.export.JRXlsAbstractExporterNature;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRXlsExporterNature extends JRXlsAbstractExporterNature
{

	/**
	 * 
	 */
	public JRXlsExporterNature(
		KlexReportsContext klexReportsContext,
		ExporterFilter filter, 
		boolean isIgnoreGraphics, 
		boolean isIgnorePageMargins
		)
	{
		super(klexReportsContext, filter, isIgnoreGraphics, isIgnorePageMargins);
	}

	@Override
	public boolean isToExport(JRPrintElement element)
	{
		boolean isToExport = true;
		if (element instanceof JRGenericPrintElement)
		{
			JRGenericPrintElement genericElement = (JRGenericPrintElement) element;
			GenericElementHandler handler = handlerEnvironment.getElementHandler(
					genericElement.getGenericType(), JRXlsExporter.XLS_EXPORTER_KEY);
			if (handler == null || !handler.toExport(genericElement))
			{
				isToExport = false;
			}
		}

		return isToExport && super.isToExport(element);
	}
	
	
	@Override
	public Integer getColumnWidth(JRPrintElement element, boolean columnAutoFit) 
	{
		if (
			element.hasProperties()
			&& element.getPropertiesMap().containsProperty(ExcelAbstractExporter.PROPERTY_COLUMN_WIDTH)
			)
		{
			// we make this test to avoid reaching the global default value of the property directly
			// and thus skipping the report level one, if present
			return getPropertiesUtil().getIntegerProperty(element, ExcelAbstractExporter.PROPERTY_COLUMN_WIDTH, 0);
		}
		return null;
	}

}
