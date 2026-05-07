/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.components.iconlabel;

import java.io.IOException;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRLineBox;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.base.JRBasePrintFrame;
import net.sf.klexreports.engine.export.GenericElementXmlHandler;
import net.sf.klexreports.engine.export.JRXmlExporter;
import net.sf.klexreports.engine.export.JRXmlExporterContext;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class IconLabelElementXmlHandler implements GenericElementXmlHandler
{
	private static final IconLabelElementXmlHandler INSTANCE = new IconLabelElementXmlHandler();
	
	public static IconLabelElementXmlHandler getInstance()
	{
		return INSTANCE;
	}


	@Override
	public void exportElement(JRXmlExporterContext exporterContext, JRGenericPrintElement element) 
	{
		JRPrintText labelPrintText = (JRPrintText)element.getParameterValue(IconLabelElement.PARAMETER_LABEL_TEXT_ELEMENT);
		if (labelPrintText == null)
		{
			return;
		}

		JRBasePrintFrame frame = new JRBasePrintFrame(element.getDefaultStyleProvider());
		frame.setX(element.getX());
		frame.setY(element.getY());
		frame.setWidth(element.getWidth());
		frame.setHeight(element.getHeight());
		frame.setStyle(element.getStyle());
		frame.setBackcolor(element.getBackcolor());
		frame.setForecolor(element.getForecolor());
		frame.setMode(element.getMode());
		JRLineBox lineBox = (JRLineBox)element.getParameterValue(IconLabelElement.PARAMETER_LINE_BOX);
		if (lineBox != null)
		{
			frame.copyBox(lineBox);
		}
		
		frame.addElement(labelPrintText);
		
		JRPrintText iconPrintText = (JRPrintText)element.getParameterValue(IconLabelElement.PARAMETER_ICON_TEXT_ELEMENT);
		if (iconPrintText != null)
		{
			frame.addElement(iconPrintText);
		}

		JRXmlExporter exporter = (JRXmlExporter)exporterContext.getExporterRef();
		
		try
		{
			exporter.exportElement(frame);
		}
		catch (JRException | IOException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public boolean toExport(JRGenericPrintElement element) 
	{
		return true;
	}
	
}
