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

import net.sf.klexreports.engine.JROrigin;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.PrintBookmark;
import net.sf.klexreports.engine.SimplePrintPageFormat;
import net.sf.klexreports.engine.SimplePrintPart;
import net.sf.klexreports.engine.base.BasePrintBookmark;
import net.sf.klexreports.engine.base.JRBasePrintPage;
import net.sf.klexreports.engine.type.BandTypeEnum;
import net.sf.klexreports.engine.type.OrientationEnum;
import net.sf.klexreports.engine.xml.JRXmlConstants;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class KlexPrintLoader
{
	
	private static final KlexPrintLoader INSTANCE = new KlexPrintLoader();
	
	public static KlexPrintLoader instance()
	{
		return INSTANCE;
	}

	public KlexPrint load(XmlLoader xmlLoader)
	{
		KlexPrint klexPrint = new KlexPrint();
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_name, klexPrint::setName);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_pageWidth, klexPrint::setPageWidth);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_pageHeight, klexPrint::setPageHeight);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_topMargin, klexPrint::setTopMargin);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_leftMargin, klexPrint::setLeftMargin);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_bottomMargin, klexPrint::setBottomMargin);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_rightMargin, klexPrint::setRightMargin);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_orientation, OrientationEnum::getByName, klexPrint::setOrientation);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_formatFactoryClass, klexPrint::setFormatFactoryClass);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_locale, klexPrint::setLocaleCode);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_timezone, klexPrint::setTimeZoneId);
		
		xmlLoader.loadElements(element ->
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_property:
				PropertyLoader.instance().loadProperty(xmlLoader, klexPrint);
				break;
			case JRXmlConstants.ELEMENT_origin:
				JROrigin origin = loadOrigin(xmlLoader);
				klexPrint.addOrigin(origin);
				break;
			case JRXmlConstants.ELEMENT_style:
				StyleLoader.instance().loadStyle(xmlLoader, klexPrint);
				break;
			case JRXmlConstants.ELEMENT_bookmark:
				PrintBookmark bookmark = loadBookmark(xmlLoader);
				klexPrint.addBookmark(bookmark);
				break;
			case JRXmlConstants.ELEMENT_part:
				loadPart(xmlLoader, klexPrint);
				break;
			case JRXmlConstants.ELEMENT_page:
				loadPage(xmlLoader, klexPrint);
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});
		
		return klexPrint;
	}

	protected JROrigin loadOrigin(XmlLoader xmlLoader)
	{
		String report = xmlLoader.getAttribute(JRXmlConstants.ATTRIBUTE_report);
		String group = xmlLoader.getAttribute(JRXmlConstants.ATTRIBUTE_group);
		BandTypeEnum bandType = xmlLoader.getEnumAttribute(JRXmlConstants.ATTRIBUTE_band, BandTypeEnum::getByName);
		xmlLoader.endElement();
		return new JROrigin(report, group, bandType);
	}
	
	protected PrintBookmark loadBookmark(XmlLoader xmlLoader)
	{
		String label = xmlLoader.getAttribute(JRXmlConstants.ATTRIBUTE_label);
		Integer pageIndexAttr = xmlLoader.getIntAttribute(JRXmlConstants.ATTRIBUTE_pageIndex);
		int pageIndex = pageIndexAttr != null ? pageIndexAttr : 0;
		String elementAddress = xmlLoader.getAttribute(JRXmlConstants.ATTRIBUTE_elementAddress);
		BasePrintBookmark bookmark = new BasePrintBookmark(label, pageIndex, elementAddress);
		
		xmlLoader.loadElements(element ->
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_bookmark:
				PrintBookmark subBookmark = loadBookmark(xmlLoader);
				bookmark.addBookmark(subBookmark);
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});
		
		return bookmark;
	}
	
	protected void loadPart(XmlLoader xmlLoader, KlexPrint klexPrint)
	{
		SimplePrintPart part = new SimplePrintPart();
		Integer pageIndex = xmlLoader.getIntAttribute(JRXmlConstants.ATTRIBUTE_pageIndex);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_name, part::setName);
		
		SimplePrintPageFormat pageFormat = new SimplePrintPageFormat();
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_pageWidth, pageFormat::setPageWidth);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_pageHeight, pageFormat::setPageHeight);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_topMargin, pageFormat::setTopMargin);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_leftMargin, pageFormat::setLeftMargin);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_bottomMargin, pageFormat::setBottomMargin);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_rightMargin, pageFormat::setRightMargin);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_orientation, OrientationEnum::getByName, pageFormat::setOrientation);
		part.setPageFormat(pageFormat);
		
		xmlLoader.loadElements(element -> 
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_property:
				PropertyLoader.instance().loadProperty(xmlLoader, part);
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});

		klexPrint.addPart(pageIndex, part);
	}

	protected void loadPage(XmlLoader xmlLoader, KlexPrint klexPrint)
	{
		JRBasePrintPage page = new JRBasePrintPage();
		xmlLoader.loadElements(element -> 
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_line:
				LineLoader.instance().loadLine(xmlLoader, klexPrint, page::addElement);
				break;
			case JRXmlConstants.ELEMENT_rectangle:
				RectangleLoader.instance().loadRectangle(xmlLoader, klexPrint, page::addElement);
				break;
			case JRXmlConstants.ELEMENT_ellipse:
				EllipseLoader.instance().loadEllipse(xmlLoader, klexPrint, page::addElement);
				break;
			case JRXmlConstants.ELEMENT_image:
				ImageLoader.instance().loadImage(xmlLoader, klexPrint, page::addElement);
				break;
			case JRXmlConstants.ELEMENT_text:
				TextLoader.instance().loadText(xmlLoader, klexPrint, page::addElement);
				break;
			case JRXmlConstants.ELEMENT_frame:
				FrameLoader.instance().loadFrame(xmlLoader, klexPrint, page::addElement);
				break;
			case JRXmlConstants.ELEMENT_genericElement:
				GenericElementLoader.instance().loadGenericElement(xmlLoader, klexPrint, page::addElement);
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});
		klexPrint.addPage(page);
	}
}
