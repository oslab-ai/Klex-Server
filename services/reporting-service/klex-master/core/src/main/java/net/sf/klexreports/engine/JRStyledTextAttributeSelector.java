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
package net.sf.klexreports.engine;

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.klexreports.engine.fonts.FontUtil;
import net.sf.klexreports.engine.type.ModeEnum;
import net.sf.klexreports.engine.util.JRDataUtils;
import net.sf.klexreports.engine.util.JRStyledTextParser;
import net.sf.klexreports.engine.util.JRStyledTextUtil;

/**
 * Selector of element-level styled text attributes for print text objects.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see JRStyledTextUtil#getStyledText(JRPrintText, JRStyledTextAttributeSelector)
 * @see JRPrintText#getFullStyledText(JRStyledTextAttributeSelector)
 */
public abstract class JRStyledTextAttributeSelector
{
	protected final KlexReportsContext klexReportsContext;
	
	/**
	 * 
	 */
	protected JRStyledTextAttributeSelector(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	/**
	 * 
	 */
	private static Locale getLocale()
	{
		return JRStyledTextParser.getLocale();
	}
	
	/**
	 * 
	 */
	public static Locale getTextLocale(JRPrintText printText)
	{
		String localeCode = printText.getLocaleCode();
		if (localeCode == null)
		{
			return getLocale();
		}
		return JRDataUtils.getLocale(localeCode);
	}
	
	/**
	 * Construct a map containing the selected element-level styled text attributes
	 * for a print text element.
	 * 
	 * @param printText the print text object
	 * @return a map containing styled text attributes
	 */
	public abstract Map<Attribute,Object> getStyledTextAttributes(JRPrintText printText);
	

	/**
	 * Selects all styled text attributes, i.e. font attributes plus forecolor
	 * and backcolor.
	 */
	public static JRStyledTextAttributeSelector getAllSelector(KlexReportsContext klexReportsContext)
	{
		return new AllSelector(klexReportsContext);
	}
	

	/**
	 * Selects all styled text attributes, i.e. font attributes plus forecolor
	 * and backcolor.
	 */
	private static class AllSelector extends JRStyledTextAttributeSelector
	{
		public AllSelector(KlexReportsContext klexReportsContext)
		{
			super(klexReportsContext);
		}
		
		@Override
		public Map<Attribute,Object> getStyledTextAttributes(JRPrintText printText)
		{
			Map<Attribute,Object> attributes = new HashMap<>(); 
			//JRFontUtil.getAttributes(attributes, printText, getTextLocale(printText));
			FontUtil.getInstance(klexReportsContext).getAttributesWithoutAwtFont(attributes, printText);
			attributes.put(TextAttribute.FOREGROUND, printText.getForecolor());
			if (printText.getMode() == ModeEnum.OPAQUE)
			{
				attributes.put(TextAttribute.BACKGROUND, printText.getBackcolor());
			}
			return attributes;
		}
	}


	/**
	 * Selects all styled text attribute except backcolor, i.e. font attributes
	 * plus forecolor.
	 */
	public static JRStyledTextAttributeSelector getNoBackcolorSelector(KlexReportsContext klexReportsContext)
	{
		return new NoBackcolorSelector(klexReportsContext);
	}
	

	/**
	 * Selects all styled text attribute except backcolor, i.e. font attributes
	 * plus forecolor.
	 */
	private static class NoBackcolorSelector extends JRStyledTextAttributeSelector
	{
		public NoBackcolorSelector(KlexReportsContext klexReportsContext)
		{
			super(klexReportsContext);
		}
		
		@Override
		public Map<Attribute,Object> getStyledTextAttributes(JRPrintText printText)
		{
			Map<Attribute,Object> attributes = new HashMap<>(); 
			//JRFontUtil.getAttributes(attributes, printText, getTextLocale(printText));
			FontUtil.getInstance(klexReportsContext).getAttributesWithoutAwtFont(attributes, printText);
			attributes.put(TextAttribute.FOREGROUND, printText.getForecolor());
			return attributes;
		}
	}
	

	/**
	 * Doesn't select any styled text attribute.
	 */
	public static JRStyledTextAttributeSelector getNoneSelector(KlexReportsContext klexReportsContext)
	{
		return new NoneSelector(klexReportsContext);
	}
	

	/**
	 * Doesn't select any styled text attribute.
	 */
	private static class NoneSelector extends JRStyledTextAttributeSelector
	{
		public NoneSelector(KlexReportsContext klexReportsContext)
		{
			super(klexReportsContext);
		}
		
		@Override
		public Map<Attribute,Object> getStyledTextAttributes(JRPrintText printText)
		{
			return null;
		}
	}
	
}

