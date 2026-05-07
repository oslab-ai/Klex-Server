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
package net.sf.klexreports.charts.renderers;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ui.Drawable;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.renderers.AbstractRenderer;
import net.sf.klexreports.renderers.Graphics2DRenderable;


/**
 * A wrapper for the Drawable interface in the JCommon library: you will need the
 * JCommon classes in your classpath to compile this class. In particular this can be
 * used to allow JFreeChart objects to be included in the output report in vector form.
 *
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JCommonDrawableRendererImpl extends AbstractRenderer implements Graphics2DRenderable
{

	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */	
	private Drawable drawable;


	/**
	 *
	 */	
	public JCommonDrawableRendererImpl(Drawable drawable) 
	{
		this.drawable = drawable;
	}

	@Override
	public void render(KlexReportsContext klexReportsContext, Graphics2D grx, Rectangle2D rectangle) 
	{
		if (drawable != null) 
		{
			drawable.draw(grx, rectangle);
		}
	}
	
}
