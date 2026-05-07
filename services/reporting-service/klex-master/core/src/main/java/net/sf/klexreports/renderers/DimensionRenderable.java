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

import java.awt.geom.Dimension2D;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * This interface is implemented by renderable objects that want to provide a dimension for the graphics they render,
 * usually by also implementing the {@link Graphics2DRenderable} interface.
 * Data renderables such as images or SVG files do not need to provide a dimension as that will be read from the files themselves when needed.
 * Reading the dimension of images and SVG documents is performed using wrapping rendeable implementations that wrap the original data renderable and only ask them for their data.
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public interface DimensionRenderable
{
	/**
	 *
	 */
	public Dimension2D getDimension(KlexReportsContext klexReportsContext) throws JRException;
}
