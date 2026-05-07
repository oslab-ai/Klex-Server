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
package net.sf.klexreports.engine.export.ooxml;

import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.fonts.FontFace;

/**
 * @author Sanda Zaharia(shertage@users.sourceforge.net)
 */
public class DocxFontHelper extends BaseFontHelper
{
	/**
	 * 
	 */
	public DocxFontHelper(
		KlexReportsContext klexReportsContext, 
		DocxZip docxZip,
		boolean isEmbedFonts
		)
	{
		super(
			klexReportsContext, 
			docxZip.getFontTableEntry().getWriter(), 
			docxZip.getFontTableRelsEntry().getWriter(),
			docxZip,
			isEmbedFonts
			);
	}

	@Override
	protected String getExporterKey()
	{
		return JRDocxExporter.DOCX_EXPORTER_KEY;
	}

	@Override
	protected String getEndFontTag()
	{
		return "  </w:font>\n";
	}

	@Override
	protected String getFontsDir() 
	{
		return WORD_FONTS_DIR;
	}
	
	@Override
	protected String getStartFontTag(String fontName)
	{
		return "  <w:font w:name=\"" + fontName + "\">\n";
	}
	
	@Override
	protected String getRegularEmbedding(String id)
	{
		return "    <w:embedRegular r:id=\"" + id + "\"/>\n";
	}
	
	@Override
	protected String getBoldEmbedding(String id)
	{
		return "    <w:embedBold r:id=\"" + id + "\"/>\n";
	}
	
	@Override
	protected String getItalicEmbedding(String id)
	{
		return "    <w:embedItalic r:id=\"" + id + "\"/>\n";
	}
	
	@Override
	protected String getBoldItalicEmbedding(String id)
	{
		return "    <w:embedBoldItalic r:id=\"" + id + "\"/>\n";
	}

	@Override
	protected String getFontPath(FontFace fontFace)
	{
		return fontFace == null ? null : fontFace.getTtf();
	}
}
