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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.batik.bridge.DefaultFontFamilyResolver;
import org.apache.batik.bridge.FontFace;
import org.apache.batik.bridge.FontFamilyResolver;
import org.apache.batik.gvt.font.GVTFontFamily;

import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.HtmlFontFamily;
import net.sf.klexreports.engine.fonts.FontInfo;
import net.sf.klexreports.engine.fonts.FontUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class BatikFontFamilyResolver implements FontFamilyResolver
{
	/**
	 *
	 */
	private final KlexReportsContext klexReportsContext;
	private final FontUtil fontUtil;
	
	private final Map<String, GVTFontFamily> resolvedFontFamilies = new HashMap<>();


	/**
	 *
	 */
	private BatikFontFamilyResolver(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
		this.fontUtil = FontUtil.getInstance(klexReportsContext);
	}


	/**
	 *
	 */
	public static BatikFontFamilyResolver getInstance(KlexReportsContext klexReportsContext)
	{
		return new BatikFontFamilyResolver(klexReportsContext);
	}


	@Override
	public GVTFontFamily resolve(String familyName, FontFace fontFace) 
	{
		return DefaultFontFamilyResolver.SINGLETON.resolve(familyName, fontFace);
	}

	@Override
	public GVTFontFamily loadFont(InputStream in, FontFace ff) throws Exception 
	{
		return DefaultFontFamilyResolver.SINGLETON.loadFont(in, ff);
	}

	@Override
	public GVTFontFamily resolve(String familyName) 
	{
		GVTFontFamily gvtFontFamily = resolvedFontFamilies.get(familyName);
		
		if (gvtFontFamily == null)
		{
			FontInfo fontInfo = fontUtil.getFontInfo(familyName, true, null);//FIXMEBATIK locale
			
			if (fontInfo == null)
			{
				// svg font-family could have locale suffix because it is needed in svg measured by phantomjs;
				int localeSeparatorPos = familyName.lastIndexOf(HtmlFontFamily.LOCALE_SEPARATOR);
				if (localeSeparatorPos > 0)
				{
					String family = familyName.substring(0, localeSeparatorPos);
					fontInfo = fontUtil.getFontInfo(family, true, null);//FIXMEBATIK locale
				}
			}
			
			if (fontInfo != null)
			{
				gvtFontFamily = new BatikAWTFontFamily(klexReportsContext, familyName);
				resolvedFontFamilies.put(familyName, gvtFontFamily);
			}
			else
			{
				return DefaultFontFamilyResolver.SINGLETON.resolve(familyName);
			}
		}
		
		return gvtFontFamily;
	}

	@Override
	public GVTFontFamily getFamilyThatCanDisplay(char c) 
	{
		return DefaultFontFamilyResolver.SINGLETON.getFamilyThatCanDisplay(c);
	}

	@Override
	public GVTFontFamily getDefault() 
	{
		return DefaultFontFamilyResolver.SINGLETON.getDefault();
	}

}
