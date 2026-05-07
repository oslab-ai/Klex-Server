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
 * Text measurer interface.
 * 
 * A text measurer is responsible for fitting a text in a given space
 * and for computing other text measuring information.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface JRTextMeasurer
{

	/**
	 * Fit a text chunk in a given space. 
	 * 
	 * @param styledText the full text
	 * @param remainingTextStart the start index of the remaining text
	 * @param availableStretchHeight the available stretch height
	 * @param indentFirstLine whether should honor first line indent
	 * @param canOverflow whether the text element is able to overflow
	 * @return text measuring information
	 */
	JRMeasuredText measure(JRStyledText styledText,
		int remainingTextStart,
		int availableStretchHeight,
		boolean indentFirstLine,
		boolean canOverflow
		);

}
