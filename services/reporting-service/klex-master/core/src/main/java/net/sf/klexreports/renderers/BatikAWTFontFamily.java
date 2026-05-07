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
package net.sf.klexreports.renderers;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

import org.apache.batik.gvt.font.AWTFontFamily;
import org.apache.batik.gvt.font.AWTGVTFont;
import org.apache.batik.gvt.font.GVTFont;

import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.HtmlFontFamily;
import net.sf.klexreports.engine.fonts.FontUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class BatikAWTFontFamily extends AWTFontFamily
{
	private final KlexReportsContext klexReportsContext;
	
	public BatikAWTFontFamily(
		KlexReportsContext klexReportsContext,
		String familyName
		) 
	{
		super(familyName);
		
		this.klexReportsContext = klexReportsContext;
	}

	@Override
	public GVTFont deriveFont(float size, Map attrs) 
	{
		if (font != null)
			return new AWTGVTFont(font, size);

		String fontFamilyName = fontFace.getFamilyName();
		if (
			fontFamilyName.startsWith("'")
			&& fontFamilyName.endsWith("'")
			)
		{
			fontFamilyName = fontFamilyName.substring(1, fontFamilyName.length() - 1);
		}

		// svg font-family could have locale suffix because it is needed in svg measured by phantomjs;
		int localeSeparatorPos = fontFamilyName.lastIndexOf(HtmlFontFamily.LOCALE_SEPARATOR);
		if (localeSeparatorPos > 0)
		{
			fontFamilyName = fontFamilyName.substring(0, localeSeparatorPos);
		}

		Font awtFont = 
			FontUtil.getInstance(klexReportsContext).getAwtFontFromBundles(
				true,
				fontFamilyName,
				(TextAttribute.WEIGHT_BOLD.equals(attrs.get(TextAttribute.WEIGHT)) ? Font.BOLD : Font.PLAIN)
				| (TextAttribute.POSTURE_OBLIQUE.equals(attrs.get(TextAttribute.POSTURE)) ? Font.ITALIC : Font.PLAIN),
				size, 
				null,//FIXMEBATIK locale 
				true
				);
		
		if (awtFont != null)
		{
			return new AWTGVTFont(awtFont);
		}

		return super.deriveFont(size, attrs);
	}
}
