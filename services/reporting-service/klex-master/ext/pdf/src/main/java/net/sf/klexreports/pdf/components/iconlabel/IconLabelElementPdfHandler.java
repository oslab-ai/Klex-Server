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
package net.sf.klexreports.pdf.components.iconlabel;

import net.sf.klexreports.components.iconlabel.IconLabelElement;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRLineBox;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.base.JRBasePrintFrame;
import net.sf.klexreports.pdf.GenericElementPdfHandler;
import net.sf.klexreports.pdf.JRPdfExporter;
import net.sf.klexreports.pdf.JRPdfExporterContext;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class IconLabelElementPdfHandler implements GenericElementPdfHandler
{
	private static final IconLabelElementPdfHandler INSTANCE = new IconLabelElementPdfHandler();
	
	public static IconLabelElementPdfHandler getInstance()
	{
		return INSTANCE;
	}
	
	private IconLabelElementPdfHandler()
	{
	}
	
	@Override
	public void exportElement(JRPdfExporterContext exporterContext, JRGenericPrintElement element)
	{
		JRPrintText labelPrintText = (JRPrintText)element.getParameterValue(IconLabelElement.PARAMETER_LABEL_TEXT_ELEMENT);
		if (labelPrintText == null) //FIXMEINPUT deal with xml serialization
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
		if (iconPrintText != null) //FIXMEINPUT deal with xml serialization
		{
			frame.addElement(iconPrintText);
		}

		JRPdfExporter exporter = (JRPdfExporter)exporterContext.getExporterRef();
		try
		{
			exporter.exportFrame(frame);
		}
		catch(Exception e)
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
