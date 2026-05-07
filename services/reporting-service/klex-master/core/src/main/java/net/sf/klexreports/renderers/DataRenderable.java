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

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * The {@link net.sf.klexreports.renderers.DataRenderable} interface has a method called 
 * {@link DataRenderable#getData(KlexReportsContext)},
 * which gets called by the engine each time it needs the actual image data either to embed it
 * directly into the exported documents, or to use it to draw the image or the graphic onto a device 
 * or graphic context.
 *
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public interface DataRenderable
{
	/**
	 *
	 */
	public byte[] getData(KlexReportsContext klexReportsContext) throws JRException;
}
