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

import net.sf.klexreports.engine.KlexPrint;

/**
 * Listener notified of page events during a report generation.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see AsynchronousFillHandle#addFillListener(FillListener)
 */
public interface FillListener
{

	/**
	 * Called when a report page has been generated.
	 * 
	 * The page might not be final due to delayed evaluation elements.
	 * 
	 * @param klexPrint the report that is currently generating
	 * @param pageIndex the index of the page that has been generated
	 */
	void pageGenerated(KlexPrint klexPrint, int pageIndex);

	/**
	 * Called when a previously generated page has been updated,
	 * usually by evaluating a delayed element.
	 * 
	 * @param klexPrint the report that is currently generating
	 * @param pageIndex the index of the page that has been updated
	 */
	void pageUpdated(KlexPrint klexPrint, int pageIndex);

}
