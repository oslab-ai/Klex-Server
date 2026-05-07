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
import net.sf.klexreports.engine.export.ooxml.GenericElementPptxHandler;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporterContext;

/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class MapElementPptxHandler implements GenericElementPptxHandler
{
	private static final MapElementPptxHandler INSTANCE = new MapElementPptxHandler();
	
	public static MapElementPptxHandler getInstance()
	{
		return INSTANCE;
	}
	
	@Override
	public void exportElement(
		JRPptxExporterContext exporterContext,
		JRGenericPrintElement element
		)
	{
		try
		{
			JRPptxExporter exporter = (JRPptxExporter)exporterContext.getExporterRef();
			JRPrintImage mapImage = DefaultMapElementImageProvider
					.getInstance()
					.getImage(exporterContext.getKlexReportsContext(), element);

			exporter.exportImage(mapImage);
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
