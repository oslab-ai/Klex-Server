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
package net.sf.klexreports.pdf.common;

import java.awt.color.ColorSpace;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Locale;
import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.fonts.FontUtil;
import net.sf.klexreports.engine.util.JRStyledTextUtil;
import net.sf.klexreports.pdf.JRPdfExporter;
import net.sf.klexreports.pdf.type.PdfVersionEnum;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface PdfProducerContext
{
	
	JRPdfExporter getExporter();
	
	KlexReportsContext getKlexReportsContext();
	
	JRPropertiesUtil getProperties();

	FontUtil getFontUtil();

	JRStyledTextUtil getStyledTextUtil();

	boolean isTagged();

	void setMinimalVersion(PdfVersionEnum version);

	KlexPrint getCurrentKlexPrint();
	
	void setFont(Map<Attribute,Object> attributes, Locale locale, boolean setFontLines,
			FontRecipient recipient);
	
	JRException handleDocumentException(Exception e);
	
	ColorSpace getCMYKColorSpace();

}
