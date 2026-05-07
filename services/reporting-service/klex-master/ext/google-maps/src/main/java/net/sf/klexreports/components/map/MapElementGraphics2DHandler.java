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

import java.awt.Graphics2D;

import net.sf.klexreports.components.map.imageprovider.DefaultMapElementImageProvider;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.export.GenericElementGraphics2DHandler;
import net.sf.klexreports.engine.export.JRGraphics2DExporter;
import net.sf.klexreports.engine.export.JRGraphics2DExporterContext;
import net.sf.klexreports.engine.export.draw.ImageDrawer;
import net.sf.klexreports.engine.export.draw.Offset;

/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class MapElementGraphics2DHandler implements GenericElementGraphics2DHandler
{
	private static final MapElementGraphics2DHandler INSTANCE = new MapElementGraphics2DHandler();
	
	public static MapElementGraphics2DHandler getInstance()
	{
		return INSTANCE;
	}
	

	@Override
	public void exportElement(
			JRGraphics2DExporterContext exporterContext, 
			JRGenericPrintElement element, 
			Graphics2D grx, 
			Offset offset)
	{
		try
		{
			JRGraphics2DExporter exporter = (JRGraphics2DExporter)exporterContext.getExporterRef();
			ImageDrawer imageDrawer = exporter.getDrawVisitor().getImageDrawer();
			JRPrintImage mapImage = DefaultMapElementImageProvider
					.getInstance()
					.getImage(exporterContext.getKlexReportsContext(), element);
			
			imageDrawer.draw(
					grx,
					mapImage,
					offset.getX(), 
					offset.getY()
					);
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
