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

import net.sf.klexreports.engine.JRPrintLine;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.base.JRBasePrintLine;
import net.sf.klexreports.engine.type.LineDirectionEnum;
import net.sf.klexreports.engine.xml.JRXmlConstants;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class LineLoader
{
	
	private static final LineLoader INSTANCE = new LineLoader();
	
	public static LineLoader instance()
	{
		return INSTANCE;
	}

	public void loadLine(XmlLoader xmlLoader, KlexPrint klexPrint, Consumer<? super JRPrintLine> consumer)
	{
		JRBasePrintLine line = new JRBasePrintLine(klexPrint.getDefaultStyleProvider());
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_direction, LineDirectionEnum::getByName, line::setDirection);
		
		xmlLoader.loadElements(element -> 
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_reportElement:
				ReportElementLoader.instance().loadReportElement(xmlLoader, klexPrint, line);
				break;
			case JRXmlConstants.ELEMENT_graphicElement:
				ReportElementLoader.instance().loadGraphicElement(xmlLoader, line);
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});
		
		consumer.accept(line);
	}
	
}
