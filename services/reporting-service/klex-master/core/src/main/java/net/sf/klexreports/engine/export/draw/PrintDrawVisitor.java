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
package net.sf.klexreports.engine.export.draw;

import java.awt.Graphics2D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintEllipse;
import net.sf.klexreports.engine.JRPrintFrame;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.JRPrintLine;
import net.sf.klexreports.engine.JRPrintRectangle;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.PrintElementVisitor;
import net.sf.klexreports.engine.export.AwtTextRenderer;
import net.sf.klexreports.engine.export.ExporterFilter;
import net.sf.klexreports.engine.export.GenericElementGraphics2DHandler;
import net.sf.klexreports.engine.export.GenericElementHandlerEnviroment;
import net.sf.klexreports.engine.export.JRGraphics2DExporter;
import net.sf.klexreports.engine.export.JRGraphics2DExporterContext;
import net.sf.klexreports.renderers.RenderersCache;


/**
 * Print element draw visitor.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class PrintDrawVisitor implements PrintElementVisitor<Offset>
{
	private static final Log log = LogFactory.getLog(PrintDrawVisitor.class);
	
	private Graphics2D grx;
	private final KlexReportsContext klexReportsContext;
	private final LineDrawer lineDrawer;
	private final RectangleDrawer rectangleDrawer;
	private final EllipseDrawer ellipseDrawer;
	private final ImageDrawer imageDrawer;
	private TextDrawer textDrawer;
	private FrameDrawer frameDrawer;

	public PrintDrawVisitor(
		KlexReportsContext klexReportsContext,
		RenderersCache renderersCache,
		boolean minimizePrinterJobSize,
		boolean ignoreMissingFont,
		boolean defaultIndentFirstLine,
		boolean defaultJustifyLastLine
		)
	{
		this.klexReportsContext = klexReportsContext;
		this.lineDrawer = new LineDrawer(klexReportsContext);
		this.rectangleDrawer = new RectangleDrawer(klexReportsContext);
		this.ellipseDrawer = new EllipseDrawer(klexReportsContext);
		this.imageDrawer = new ImageDrawer(klexReportsContext, renderersCache);

		AwtTextRenderer textRenderer = 
			new AwtTextRenderer(
				klexReportsContext,
				minimizePrinterJobSize,
				ignoreMissingFont,
				defaultIndentFirstLine,
				defaultJustifyLastLine
				);
		
		textDrawer = new TextDrawer(klexReportsContext, textRenderer);
		frameDrawer = new FrameDrawer(klexReportsContext, null, this);
	}
	
	public PrintDrawVisitor(
		JRGraphics2DExporterContext exporterContext,
		ExporterFilter filter,
		RenderersCache renderersCache,
		boolean minimizePrinterJobSize,
		boolean ignoreMissingFont,
		boolean defaultIndentFirstLine,
		boolean defaultJustifyLastLine
		)
	{
		this.klexReportsContext = exporterContext.getKlexReportsContext();
		this.lineDrawer = new LineDrawer(klexReportsContext);
		this.rectangleDrawer = new RectangleDrawer(klexReportsContext);
		this.ellipseDrawer = new EllipseDrawer(klexReportsContext);
		this.imageDrawer = new ImageDrawer(klexReportsContext, renderersCache);

		AwtTextRenderer textRenderer = 
			new AwtTextRenderer(
				klexReportsContext,
				minimizePrinterJobSize,
				ignoreMissingFont,
				defaultIndentFirstLine,
				defaultJustifyLastLine
				);
		
		textDrawer = new TextDrawer(klexReportsContext, textRenderer);
		frameDrawer = new FrameDrawer(exporterContext, filter, this);
	}
		
	public void setTextDrawer(TextDrawer textDrawer)
	{
		this.textDrawer = textDrawer;
	}

	public void setClip(boolean isClip)
	{
		frameDrawer.setClip(isClip);
	}
	
	public void setGraphics2D(Graphics2D grx)
	{
		this.grx = grx;
	}

	@Override
	public void visit(JRPrintText textElement, Offset offset)
	{
		textDrawer.draw(
				grx,
				textElement, 
				offset.getX(), 
				offset.getY()
				);
	}

	@Override
	public void visit(JRPrintImage image, Offset offset)
	{
		try
		{
			imageDrawer.draw(
					grx,
					image, 
					offset.getX(), 
					offset.getY()
					);
		} 
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public void visit(JRPrintRectangle rectangle, Offset offset)
	{
		rectangleDrawer.draw(
				grx,
				rectangle, 
				offset.getX(), 
				offset.getY()
				);
	}

	@Override
	public void visit(JRPrintLine line, Offset offset)
	{
		lineDrawer.draw(
				grx,
				line, 
				offset.getX(), 
				offset.getY()
				);
	}

	@Override
	public void visit(JRPrintEllipse ellipse, Offset offset)
	{
		ellipseDrawer.draw(
				grx,
				ellipse, 
				offset.getX(), 
				offset.getY()
				);
	}

	@Override
	public void visit(JRPrintFrame frame, Offset offset)
	{
		try
		{
			frameDrawer.draw(
				grx,
				frame, 
				offset.getX(), 
				offset.getY()
				);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public void visit(JRGenericPrintElement printElement, Offset offset)
	{
		GenericElementGraphics2DHandler handler = 
			(GenericElementGraphics2DHandler)GenericElementHandlerEnviroment.getInstance(klexReportsContext).getElementHandler(
					printElement.getGenericType(), 
					JRGraphics2DExporter.GRAPHICS2D_EXPORTER_KEY
					);

		if (handler != null)
		{
			handler.exportElement(this.frameDrawer.getExporterContext(), printElement, grx, offset);
		}
		else
		{
			if (log.isDebugEnabled())
			{
				log.debug("No Graphics2D generic element handler for " 
						+ printElement.getGenericType());
			}
		}
	}

	/**
	 * @return the textDrawer
	 */
	public TextDrawer getTextDrawer()
	{
		return this.textDrawer;
	}

	/**
	 * @return the imageDrawer
	 */
	public ImageDrawer getImageDrawer()
	{
		return this.imageDrawer;
	}

	/**
	 * @return the frameDrawer
	 */
	public FrameDrawer getFrameDrawer()
	{
		return frameDrawer;
	}
}
