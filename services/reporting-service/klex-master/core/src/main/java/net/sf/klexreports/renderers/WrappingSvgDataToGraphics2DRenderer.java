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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.w3c.dom.svg.SVGDocument;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintImageAreaHyperlink;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * SVG renderer implementation based on <a href="http://xmlgraphics.apache.org/batik/">Batik</a>.
 *
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class WrappingSvgDataToGraphics2DRenderer extends AbstractSvgDataToGraphics2DRenderer
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private final DataRenderable dataRenderer;
	private final AreaHyperlinksRenderable areaHyperlinksRenderer;

	/**
	 *
	 */
	public WrappingSvgDataToGraphics2DRenderer(DataRenderable dataRenderer)
	{
		super(null);
		
		this.dataRenderer = dataRenderer;
		this.areaHyperlinksRenderer = dataRenderer instanceof AreaHyperlinksRenderable ? (AreaHyperlinksRenderable)dataRenderer : null;
	}

	@Override
	protected SVGDocument getSvgDocument(
		KlexReportsContext klexReportsContext,
		SVGDocumentFactory documentFactory
		) throws JRException
	{
		try
		{
			return 
				documentFactory.createSVGDocument(
					null, 
					new ByteArrayInputStream(
						dataRenderer.getData(klexReportsContext)
						)
					);
		}
		catch (IOException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public List<JRPrintImageAreaHyperlink> getImageAreaHyperlinks(Rectangle2D renderingArea) throws JRException
	{
		return areaHyperlinksRenderer == null ? super.getImageAreaHyperlinks(renderingArea) : areaHyperlinksRenderer.getImageAreaHyperlinks(renderingArea);
	}

	@Override
	public boolean hasImageAreaHyperlinks()
	{
		return areaHyperlinksRenderer == null ? super.hasImageAreaHyperlinks() : areaHyperlinksRenderer.hasImageAreaHyperlinks();
	}

	@Override
	public byte[] getData(KlexReportsContext klexReportsContext) throws JRException 
	{
		return dataRenderer.getData(klexReportsContext);
	}

	@Override
	public int getImageDataDPI(KlexReportsContext klexReportsContext)
	{
		if (dataRenderer instanceof RenderToImageAwareRenderable)
		{
			return ((RenderToImageAwareRenderable) dataRenderer).getImageDataDPI(klexReportsContext);
		}
		
		return super.getImageDataDPI(klexReportsContext);
	}

	@Override
	public Graphics2D createGraphics(BufferedImage bi)
	{
		if (dataRenderer instanceof RenderToImageAwareRenderable)
		{
			return ((RenderToImageAwareRenderable) dataRenderer).createGraphics(bi);
		}
		
		return super.createGraphics(bi);
	}
}
