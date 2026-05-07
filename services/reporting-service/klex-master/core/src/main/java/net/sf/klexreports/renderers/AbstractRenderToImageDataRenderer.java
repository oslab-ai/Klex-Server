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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.type.ImageTypeEnum;
import net.sf.klexreports.engine.util.JRImageLoader;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class AbstractRenderToImageDataRenderer extends AbstractRenderToImageAwareRenderer implements Graphics2DRenderable, DataRenderable, DimensionRenderable
{
	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	public static final String EXCEPTION_MESSAGE_KEY_DIMENSION_MUST_OVERRIDE = "engine.renderable.svg.dimension.must.override";
	public static final String EXCEPTION_MESSAGE_KEY_DIMENSION_NULL_NOT_ALLOWED = "engine.renderable.svg.dimension.null.not.allowed";

	
	@Override
	public Dimension2D getDimension(KlexReportsContext klexReportsContext) throws JRException
	{
		throw 
			new JRException(
				EXCEPTION_MESSAGE_KEY_DIMENSION_MUST_OVERRIDE, 
				new Object[]{this.getClass().getName()}
				);
	}


	@Override
	public byte[] getData(KlexReportsContext klexReportsContext) throws JRException
	{
		Dimension2D dimension = getDimension(klexReportsContext);
		
		if (dimension == null)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_DIMENSION_NULL_NOT_ALLOWED, 
					(Object[])null
					);
		}

		int dpi = getImageDataDPI(klexReportsContext);
		double scale = dpi/72d;
		
		BufferedImage bi =
			new BufferedImage(
				(int) (scale * dimension.getWidth()),
				(int) (scale * dimension.getHeight()),
				BufferedImage.TYPE_INT_ARGB // always produce PNGs with transparency
				);

		Graphics2D g = createGraphics(bi);
		try
		{
			g.scale(scale, scale);
			Color backcolor = getBackcolor();
			if (backcolor != null)
			{
				g.setColor(backcolor);
				g.fillRect(0, 0, (int)dimension.getWidth(), (int)dimension.getHeight());
			}
			render(klexReportsContext, g, new Rectangle((int)dimension.getWidth(), (int)dimension.getHeight()));
		}
		finally
		{
			g.dispose();
		}
		
		return JRImageLoader.getInstance(klexReportsContext).loadBytesFromAwtImage(bi, ImageTypeEnum.PNG); // always produce PNGs with transparency
	}


	protected Color getBackcolor()
	{
		return null;
	}
}
