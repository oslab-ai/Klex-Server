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
package net.sf.klexreports.components.map;

import net.sf.klexreports.components.map.imageprovider.DefaultMapElementImageProvider;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.export.JRExporterGridCell;
import net.sf.klexreports.engine.export.JRGridLayout;
import net.sf.klexreports.poi.export.GenericElementXlsHandler;
import net.sf.klexreports.poi.export.JRXlsExporter;
import net.sf.klexreports.poi.export.JRXlsExporterContext;

/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class MapElementXlsHandler implements GenericElementXlsHandler
{
	private static final MapElementXlsHandler INSTANCE = new MapElementXlsHandler();
	
	public static MapElementXlsHandler getInstance()
	{
		return INSTANCE;
	}
	
	@Override
	public void exportElement(
		JRXlsExporterContext exporterContext,
		JRGenericPrintElement element,
		JRExporterGridCell gridCell,
		int colIndex,
		int rowIndex,
		int emptyCols,
		int yCutsRow, 
		JRGridLayout layout
		)
	{
		try
		{
			JRXlsExporter exporter = (JRXlsExporter)exporterContext.getExporterRef();
			JRPrintImage mapImage = DefaultMapElementImageProvider
					.getInstance()
					.getImage(exporterContext.getKlexReportsContext(), element);

			exporter.exportImage(mapImage, gridCell, colIndex, rowIndex, emptyCols, yCutsRow, layout);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean toExport(JRGenericPrintElement element) {
		return true;
	}

}
