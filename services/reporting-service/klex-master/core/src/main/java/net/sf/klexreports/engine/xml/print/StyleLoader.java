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

import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.JRStyle;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.design.JRDesignStyle;
import net.sf.klexreports.engine.type.FillEnum;
import net.sf.klexreports.engine.type.HorizontalImageAlignEnum;
import net.sf.klexreports.engine.type.HorizontalTextAlignEnum;
import net.sf.klexreports.engine.type.ModeEnum;
import net.sf.klexreports.engine.type.RotationEnum;
import net.sf.klexreports.engine.type.ScaleImageEnum;
import net.sf.klexreports.engine.type.VerticalImageAlignEnum;
import net.sf.klexreports.engine.type.VerticalTextAlignEnum;
import net.sf.klexreports.engine.xml.JRXmlConstants;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class StyleLoader
{
	
	public static final String EXCEPTION_MESSAGE_KEY_UNKNOWN_REPORT_STYLE = "xml.base.factory.unknown.report.style";
	
	private static final StyleLoader INSTANCE = new StyleLoader();
	
	public static StyleLoader instance()
	{
		return INSTANCE;
	}
	
	public void loadStyle(XmlLoader xmlLoader, KlexPrint klexPrint)
	{
		JRDesignStyle style = new JRDesignStyle(klexPrint.getDefaultStyleProvider());

		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_name, style::setName);
		xmlLoader.setBooleanAttribute(JRXmlConstants.ATTRIBUTE_isDefault, style::setDefault);

		String parentStyle = xmlLoader.getAttribute(JRXmlConstants.ATTRIBUTE_style);
		if (parentStyle != null)
		{
			Map<String, JRStyle> stylesMap = klexPrint.getStylesMap();
			JRStyle parent = stylesMap.get(parentStyle);
			if (parent == null)
			{
				throw new JRRuntimeException(EXCEPTION_MESSAGE_KEY_UNKNOWN_REPORT_STYLE,
						new Object[]{parentStyle});
			}
			style.setParentStyle(parent);
		}

		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_mode, ModeEnum::getByName, style::setMode);
		xmlLoader.setColorAttribute(JRXmlConstants.ATTRIBUTE_forecolor, style::setForecolor);
		xmlLoader.setColorAttribute(JRXmlConstants.ATTRIBUTE_backcolor, style::setBackcolor);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_fill, FillEnum::getByName, style::setFill);
		xmlLoader.setIntAttribute(JRXmlConstants.ATTRIBUTE_radius, style::setRadius);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_scaleImage, ScaleImageEnum::getByName, style::setScaleImage);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_hTextAlign, HorizontalTextAlignEnum::getByName, style::setHorizontalTextAlign);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_vTextAlign, VerticalTextAlignEnum::getByName, style::setVerticalTextAlign);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_hImageAlign, HorizontalImageAlignEnum::getByName, style::setHorizontalImageAlign);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_vImageAlign, VerticalImageAlignEnum::getByName, style::setVerticalImageAlign);
		xmlLoader.setEnumAttribute(JRXmlConstants.ATTRIBUTE_rotation, RotationEnum::getByName, style::setRotation);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_markup, style::setMarkup);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_pattern, style::setPattern);
		xmlLoader.setBooleanAttribute(JRXmlConstants.ATTRIBUTE_isBlankWhenNull, style::setBlankWhenNull);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_fontName, style::setFontName);
		xmlLoader.setBooleanAttribute(JRXmlConstants.ATTRIBUTE_isBold, style::setBold);
		xmlLoader.setBooleanAttribute(JRXmlConstants.ATTRIBUTE_isItalic, style::setItalic);
		xmlLoader.setBooleanAttribute(JRXmlConstants.ATTRIBUTE_isUnderline, style::setUnderline);
		xmlLoader.setBooleanAttribute(JRXmlConstants.ATTRIBUTE_isStrikeThrough, style::setStrikeThrough);
		xmlLoader.setFloatAttribute(JRXmlConstants.ATTRIBUTE_fontSize, style::setFontSize);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_pdfFontName, style::setPdfFontName);
		xmlLoader.setAttribute(JRXmlConstants.ATTRIBUTE_pdfEncoding, style::setPdfEncoding);
		xmlLoader.setBooleanAttribute(JRXmlConstants.ATTRIBUTE_isPdfEmbedded, style::setPdfEmbedded);

		xmlLoader.loadElements(element ->
		{
			switch (element)
			{
			case JRXmlConstants.ELEMENT_pen:
				PenLoader.instance().loadPen(xmlLoader, style.getLinePen());
				break;
			case JRXmlConstants.ELEMENT_box:
				BoxLoader.instance().loadBox(xmlLoader, style.getLineBox());
				break;
			case JRXmlConstants.ELEMENT_paragraph:
				ParagraphsLoader.instance().loadParagraph(xmlLoader, style.getParagraph());
				break;
			default:
				xmlLoader.unexpectedElement(element);
				break;
			}
		});
		
		try
		{
			klexPrint.addStyle(style);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}
	
}
