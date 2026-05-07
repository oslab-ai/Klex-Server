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
package net.sf.klexreports.engine.fill;

import net.sf.klexreports.engine.util.JRStyledText;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface TextLineWrapper
{

	void init(TextMeasureContext context);
	
	boolean start(JRStyledText styledText);
	
	void startParagraph(int paragraphStart, int paragraphEnd, boolean truncateAtChar);

	void startEmptyParagraph(int paragraphStart);
	
	int paragraphPosition();
	
	int paragraphEnd();

	TextLine nextLine(float width, int endLimit, boolean requireWord);

	TextLine baseTextLine(int index);

	float maxFontsize(int start, int end);

	String getLineText(int start, int end);
	
	char charAt(int index);
	
	TextLineWrapper lastLineWrapper(String lineText, int start, int textLength, boolean truncateAtChar);

}
