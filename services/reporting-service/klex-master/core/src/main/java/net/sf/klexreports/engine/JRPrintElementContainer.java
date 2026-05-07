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

/**
 * Print element container interface.
 * <p>
 * An instance of this type is used by {@link net.sf.klexreports.engine.fill.JRFillElementContainer JRFillElementContainer}
 * to collect the generated print elements.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface JRPrintElementContainer extends JRPrintElementListContainer
{
	/**
	 * Returns the height of the container.
	 * 
	 * @return the height of the container
	 */
	public int getHeight();
	
	
	/**
	 * Sets the height of the container.
	 * 
	 * @param height the height
	 */
	public void setHeight(int height);
	
	default void setContentsWidth(int width)
	{
		//NOOP
	}
}
