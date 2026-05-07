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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.apache.batik.ext.awt.image.GraphicsUtil;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintImageAreaHyperlink;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * This class a renderable implementation for wrapping SVG data.
 * It is better suited for SVG data then the {@link SimpleDataRenderer} because it it allows providing additional 
 * information about required resolution of the graphic, when converted to
 * and image, as the engine needs to do that for certain document formats at export time.
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class SimpleRenderToImageAwareDataRenderer extends AbstractRenderToImageAwareRenderer implements DataRenderable, AreaHyperlinksRenderable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private final byte[] data;
	private List<JRPrintImageAreaHyperlink> areaHyperlinks;

	private int minDPI;
	private boolean antiAlias;

	
	/**
	 *
	 */
	protected SimpleRenderToImageAwareDataRenderer(byte[] data)
	{
		this(data, null);
	}

	/**
	 * Creates a SVG renderer.
	 *
	 * @param areaHyperlinks a list of {@link JRPrintImageAreaHyperlink area hyperlinks}
	 */
	public SimpleRenderToImageAwareDataRenderer(
		byte[] data,
		List<JRPrintImageAreaHyperlink> areaHyperlinks
		)
	{
		this.data = data;
		this.areaHyperlinks = areaHyperlinks;
	}

	/**
	 *
	 */
	public static SimpleRenderToImageAwareDataRenderer getInstance(byte[] data)
	{
		if (data != null) 
		{
			return new SimpleRenderToImageAwareDataRenderer(data);
		}
		return null;
	}


	@Override
	public byte[] getData(KlexReportsContext klexReportsContext) throws JRException
	{
		return data;
	}

	@Override
	public List<JRPrintImageAreaHyperlink> getImageAreaHyperlinks(Rectangle2D renderingArea) throws JRException
	{
		return areaHyperlinks;
	}

	@Override
	public boolean hasImageAreaHyperlinks()
	{
		return areaHyperlinks != null && !areaHyperlinks.isEmpty();
	}

	@Override
	public int getImageDataDPI(KlexReportsContext klexReportsContext)
	{
		int dpi = super.getImageDataDPI(klexReportsContext);
		
		int lcMinDPI = getMinDPI();
		if (lcMinDPI > 0 && dpi < lcMinDPI)
		{
			dpi = lcMinDPI;
		}
		return dpi;
	}

	@Override
	public Graphics2D createGraphics(BufferedImage bi)
	{
		Graphics2D graphics = GraphicsUtil.createGraphics(bi);
		if (isAntiAlias())
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			//FIXME use JPG instead of PNG for smaller size?
		}
		return graphics;
	}

	public int getMinDPI()
	{
		return minDPI;
	}

	public void setMinDPI(int minDPI)
	{
		this.minDPI = minDPI;
	}

	public boolean isAntiAlias()
	{
		return antiAlias;
	}

	public void setAntiAlias(boolean antiAlias)
	{
		this.antiAlias = antiAlias;
	}
}
