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
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class WrappingRenderToImageDataRenderer extends AbstractRenderToImageDataRenderer
{

	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */
	private final Graphics2DRenderable renderer;
	private final DimensionRenderable dimensionRenderer;
	private final Dimension2D dimension;
	private final Color backcolor;

	
	/**
	 *
	 */
	public WrappingRenderToImageDataRenderer(
		Graphics2DRenderable renderer, 
		Dimension2D dimension,
		Color backcolor
		)
	{
		this.renderer = renderer;
		this.dimension = dimension;
		this.dimensionRenderer = null;
		this.backcolor = backcolor;
	}

	
	/**
	 *
	 */
	public WrappingRenderToImageDataRenderer(
		Graphics2DRenderable renderer, 
		DimensionRenderable dimensionRender,
		Color backcolor
		)
	{
		this.renderer = renderer;
		this.dimension = null;
		this.dimensionRenderer = dimensionRender;
		this.backcolor = backcolor;
	}

	
	@Override
	public Dimension2D getDimension(KlexReportsContext klexReportsContext)
	{
		Dimension2D imageDimension = null;
		
		if (renderer instanceof DimensionRenderable)
		{
			try
			{
				// use original dimension if possible
				imageDimension = ((DimensionRenderable)renderer).getDimension(klexReportsContext);
			}
			catch (JRException e)
			{
				// ignore
			}
		}
		
		if (
			imageDimension == null
			&& dimensionRenderer != null
			)
		{
			// fallback to supplied dimension renderable
			try
			{
				imageDimension = dimensionRenderer.getDimension(klexReportsContext);
			}
			catch (JRException e)
			{
				// ignore
			}
		}
		
		if (imageDimension == null)
		{
			// fallback to supplied dimension
			imageDimension = dimension;
		}
		
		return imageDimension;
	}

	@Override
	public Color getBackcolor()
	{
		return backcolor;
	}

	@Override
	public void render(KlexReportsContext klexReportsContext, Graphics2D grx, Rectangle2D rectangle) throws JRException
	{
		renderer.render(klexReportsContext, grx, rectangle);
	}

	@Override
	public int getImageDataDPI(KlexReportsContext klexReportsContext)
	{
		if (renderer instanceof RenderToImageAwareRenderable)
		{
			return ((RenderToImageAwareRenderable) renderer).getImageDataDPI(klexReportsContext);
		}
		
		return super.getImageDataDPI(klexReportsContext);
	}

	@Override
	public Graphics2D createGraphics(BufferedImage bi)
	{
		if (renderer instanceof RenderToImageAwareRenderable)
		{
			return ((RenderToImageAwareRenderable) renderer).createGraphics(bi);
		}
		
		return super.createGraphics(bi);
	}
}
