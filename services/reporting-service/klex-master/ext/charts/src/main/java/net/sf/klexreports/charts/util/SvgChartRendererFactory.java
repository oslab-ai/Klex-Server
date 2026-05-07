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
package net.sf.klexreports.charts.util;

import java.awt.geom.Rectangle2D;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import net.sf.klexreports.engine.JRPrintImageAreaHyperlink;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.renderers.Renderable;
import net.sf.klexreports.renderers.SimpleRenderToImageAwareDataRenderer;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class SvgChartRendererFactory extends AbstractChartRenderableFactory
{
	
	@Override
	public Renderable getRenderable(
		KlexReportsContext klexReportsContext,
		JFreeChart chart, 
		ChartHyperlinkProvider chartHyperlinkProvider,
		Rectangle2D rectangle
		)
	{
		DOMImplementation domImpl = 
			GenericDOMImplementation.getDOMImplementation();
		Document document = 
			domImpl.createDocument(null, "svg", null);
		SVGGraphics2D grx = 
			new SVGGraphics2D(document);
		
		grx.setSVGCanvasSize(rectangle.getBounds().getSize());

		List<JRPrintImageAreaHyperlink> areaHyperlinks = null;

		if (chartHyperlinkProvider != null && chartHyperlinkProvider.hasHyperlinks())
		{
			areaHyperlinks = ChartUtil.getImageAreaHyperlinks(chart, chartHyperlinkProvider, grx, rectangle);
		}
		else
		{
			chart.draw(grx, rectangle);
		}

		try
		{
			StringWriter swriter = new StringWriter();
			grx.stream(swriter);
			byte[] svgData = swriter.getBuffer().toString().getBytes(StandardCharsets.UTF_8);
			return new SimpleRenderToImageAwareDataRenderer(svgData, areaHyperlinks);
		}
		catch (SVGGraphics2DIOException e)
		{
			throw new JRRuntimeException(e);
		}
	}

}
