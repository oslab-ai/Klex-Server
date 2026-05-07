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

import net.sf.klexreports.engine.JRParagraph;
import net.sf.klexreports.engine.TabStop;
import net.sf.klexreports.engine.type.LineSpacingEnum;
import net.sf.klexreports.engine.type.TabStopAlignEnum;
import net.sf.klexreports.engine.xml.JRXmlConstants;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ParagraphsLoader
{
	
	private static final ParagraphsLoader INSTANCE = new ParagraphsLoader();
	
	public static ParagraphsLoader instance()
	{
		return INSTANCE;
	}

	public void loadParagraph(XmlLoader xmlLoader, JRParagraph paragraph)
	{
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_lineSpacing, LineSpacingEnum::getByName, paragraph::setLineSpacing);
		xmlLoader.setFloatAttribute(JRXmlConstants.ATTRIBUTE_lineSpacingSize, paragraph::setLineSpacingSize);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_firstLineIndent, paragraph::setFirstLineIndent);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_leftIndent, paragraph::setLeftIndent);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_rightIndent, paragraph::setRightIndent);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_spacingBefore, paragraph::setSpacingBefore);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_spacingAfter, paragraph::setSpacingAfter);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_tabStopWidth, paragraph::setTabStopWidth);
		
		xmlLoader.loadElements(element -> 
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_tabStop:
				TabStop tabStop = new TabStop();
				xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_position, tabStop::setPosition);
				xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_alignment, TabStopAlignEnum::getByName, tabStop::setAlignment);
				xmlLoader.endElement();
				paragraph.addTabStop(tabStop);
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});
	}
	
}
