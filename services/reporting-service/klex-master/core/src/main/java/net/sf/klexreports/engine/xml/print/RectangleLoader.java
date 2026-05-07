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
package net.sf.klexreports.engine.xml.print;

import java.util.function.Consumer;

import net.sf.klexreports.engine.JRPrintRectangle;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.base.JRBasePrintRectangle;
import net.sf.klexreports.engine.xml.JRXmlConstants;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class RectangleLoader
{
	
	private static final RectangleLoader INSTANCE = new RectangleLoader();
	
	public static RectangleLoader instance()
	{
		return INSTANCE;
	}

	public void loadRectangle(XmlLoader xmlLoader, KlexPrint klexPrint, Consumer<? super JRPrintRectangle> consumer)
	{
		JRBasePrintRectangle rectangle = new JRBasePrintRectangle(klexPrint.getDefaultStyleProvider());
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_radius, rectangle::setRadius);
		
		xmlLoader.loadElements(element -> 
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_reportElement:
				ReportElementLoader.instance().loadReportElement(xmlLoader, klexPrint, rectangle);
				break;
			case JRXmlConstants.ELEMENT_graphicElement:
				ReportElementLoader.instance().loadGraphicElement(xmlLoader, rectangle);
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});
		
		consumer.accept(rectangle);
	}
	
}
