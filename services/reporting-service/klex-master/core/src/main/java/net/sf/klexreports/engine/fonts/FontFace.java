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
package net.sf.klexreports.engine.fonts;

import java.awt.Font;



/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public interface FontFace
{

	/**
	 * Returns the non-null name of the font face.
	 */
	public String getName();
	
	/**
	 * Returns the TTF file, if the font face has specified one (the font face does not specify a TTF file in case it maps
	 * a JVM available font such as a Java logical font or a system font).
	 */
	public String getTtf();
	
	/**
	 * Returns a non-null instance of java.awt.Font representing the font face.
	 */
	public Font getFont();

	/**
	 * 
	 */
	public String getPdf();

	/**
	 * 
	 */
	public String getEot();
	
	/**
	 * 
	 */
	public String getSvg();
	
	/**
	 * 
	 */
	public String getWoff();

}
