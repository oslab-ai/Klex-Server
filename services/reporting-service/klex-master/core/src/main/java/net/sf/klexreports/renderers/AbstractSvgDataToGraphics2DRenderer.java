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
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.List;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.FontFamilyResolver;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.ViewBox;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.gvt.GraphicsNode;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGPreserveAspectRatio;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintImageAreaHyperlink;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.SimpleDimension2D;


/**
 * SVG renderer implementation based on <a href="http://xmlgraphics.apache.org/batik/">Batik</a>.
 *
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class AbstractSvgDataToGraphics2DRenderer extends AbstractRenderToImageAwareRenderer implements DataRenderable, Graphics2DRenderable, DimensionRenderable, AreaHyperlinksRenderable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private List<JRPrintImageAreaHyperlink> areaHyperlinks;

	private transient SoftReference<GraphicsNode> rootNodeRef;
	private transient Dimension2D documentSize;

	/**
	 * Creates a SVG renderer.
	 *
	 * @param areaHyperlinks a list of {@link JRPrintImageAreaHyperlink area hyperlinks}
	 */
	protected AbstractSvgDataToGraphics2DRenderer(List<JRPrintImageAreaHyperlink> areaHyperlinks)
	{
		this.areaHyperlinks = areaHyperlinks;
	}

	@Override
	public Graphics2D createGraphics(BufferedImage bi)
	{
		Graphics2D graphics = GraphicsUtil.createGraphics(bi);
		return graphics;
	}

	@Override
	public void render(KlexReportsContext klexReportsContext, Graphics2D grx, Rectangle2D rectangle) throws JRException
	{
		GraphicsNode rootNode = getRootNode(klexReportsContext);

		AffineTransform transform = 
			ViewBox.getPreserveAspectRatioTransform(
				new float[]{0, 0, (float) documentSize.getWidth(), (float) documentSize.getHeight()},
				SVGPreserveAspectRatio.SVG_PRESERVEASPECTRATIO_NONE, 
				true,
				(float) rectangle.getWidth(), 
				(float) rectangle.getHeight()
				);
		Graphics2D graphics = (Graphics2D) grx.create();
		try
		{
			graphics.translate(rectangle.getX(), rectangle.getY());
			graphics.transform(transform);

			// CompositeGraphicsNode not thread safe
			synchronized (rootNode)
			{
				rootNode.paint(graphics);
			}
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Override
	public Dimension2D getDimension(KlexReportsContext klexReportsContext)
	{
		try
		{
			getRootNode(klexReportsContext);
			return documentSize;
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	protected synchronized GraphicsNode getRootNode(KlexReportsContext klexReportsContext) throws JRException
	{
		if (rootNodeRef == null || rootNodeRef.get() == null)
		{
			FontFamilyResolver fontFamilyResolver = BatikFontFamilyResolver.getInstance(klexReportsContext);
			
			UserAgent userAgentForDoc = 
				new BatikUserAgent(
					fontFamilyResolver,
					BatikUserAgent.PIXEL_TO_MM_72_DPI
					);
			
			SVGDocumentFactory documentFactory =
				new SAXSVGDocumentFactory(userAgentForDoc.getXMLParserClassName(), true);
			documentFactory.setValidating(userAgentForDoc.isXMLParserValidating());

			SVGDocument document = getSvgDocument(klexReportsContext, documentFactory);

			Node svgNode = document.getElementsByTagName("svg").item(0);
			Node svgWidthNode = svgNode.getAttributes().getNamedItem("width");
			Node svgHeightNode = svgNode.getAttributes().getNamedItem("height");
			String strSvgWidth = svgWidthNode == null ? null : svgWidthNode.getNodeValue().trim();
			String strSvgHeight = svgHeightNode == null ? null : svgHeightNode.getNodeValue().trim();
			
			float pixel2mm = BatikUserAgent.PIXEL_TO_MM_72_DPI;
			if (
				(strSvgWidth != null && strSvgWidth.endsWith("mm"))
				|| (strSvgHeight != null && strSvgHeight.endsWith("mm"))
				)
			{
				pixel2mm = BatikUserAgent.PIXEL_TO_MM_96_DPI;
			}
			
			UserAgent userAgentForCtx = 
				new BatikUserAgent(
					fontFamilyResolver,
					pixel2mm
					);
				
			BridgeContext ctx = new BridgeContext(userAgentForCtx);
			ctx.setDynamic(true);
			GVTBuilder builder = new GVTBuilder();
			GraphicsNode rootNode = builder.build(ctx, document);
			rootNodeRef = new SoftReference<>(rootNode);
			
			//copying the document size object because it has a reference to SVGSVGElementBridge,
			//which prevents rootNodeRef from being cleared by the garbage collector
			Dimension2D svgSize = ctx.getDocumentSize();
			documentSize = new SimpleDimension2D(svgSize.getWidth(), svgSize.getHeight());
		}
		
		return rootNodeRef.get();
	}
	

	/**
	 * 
	 */
	protected abstract SVGDocument getSvgDocument(
		KlexReportsContext klexReportsContext,
		SVGDocumentFactory documentFactory
		) throws JRException;

	
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
}
