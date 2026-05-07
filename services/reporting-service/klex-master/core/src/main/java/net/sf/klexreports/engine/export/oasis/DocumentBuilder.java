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

/*
 * Special thanks to Google 'Summer of Code 2005' program for supporting this development
 * 
 * Contributors:
 * Majid Ali Khan - majidkk@users.sourceforge.net
 * Frank Schönheit - Frank.Schoenheit@Sun.COM
 */
package net.sf.klexreports.engine.export.oasis;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.klexreports.engine.JRAbstractExporter;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintElementIndex;
import net.sf.klexreports.engine.JRPrintHyperlink;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.JRExporterGridCell;
import net.sf.klexreports.engine.export.JRHyperlinkProducer;
import net.sf.klexreports.engine.export.zip.FileBufferedZipEntry;
import net.sf.klexreports.engine.type.ImageTypeEnum;
import net.sf.klexreports.engine.util.JRStyledText;
import net.sf.klexreports.engine.util.JRTypeSniffer;
import net.sf.klexreports.renderers.DataRenderable;
import net.sf.klexreports.renderers.Renderable;
import net.sf.klexreports.renderers.RenderersCache;
import net.sf.klexreports.renderers.WrappingImageDataToGraphics2DRenderer;
import net.sf.klexreports.renderers.WrappingRenderToImageDataRenderer;
import net.sf.klexreports.renderers.util.RendererUtil;



/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class DocumentBuilder 
{
	/**
	 *
	 */
	protected static final String JR_PAGE_ANCHOR_PREFIX = "JR_PAGE_ANCHOR_";
	public static final String IMAGE_NAME_PREFIX = "img_";
	protected static final int IMAGE_NAME_PREFIX_LEGTH = IMAGE_NAME_PREFIX.length();
	
	/**
	 *
	 */
	protected final Map<String, String> rendererToImagePathMap = new HashMap<>();
	protected final RenderersCache renderersCache = new RenderersCache(getKlexReportsContext());
	protected final OasisZip oasisZip;
	
	
	/**
	 *
	 */
	public DocumentBuilder(OasisZip oasisZip)
	{
		this.oasisZip = oasisZip;
	}
	
	
	/**
	 *
	 */
	public static String getImageName(JRPrintElementIndex printElementIndex)
	{
		return IMAGE_NAME_PREFIX + printElementIndex.toString();
	}

	/**
	 *
	 */
	public static JRPrintElementIndex getPrintElementIndex(String imageName)
	{
		if (!imageName.startsWith(IMAGE_NAME_PREFIX))
		{
			throw 
				new JRRuntimeException(
					JRAbstractExporter.EXCEPTION_MESSAGE_KEY_INVALID_IMAGE_NAME,
					new Object[]{imageName});
		}

		return JRPrintElementIndex.parsePrintElementIndex(imageName.substring(IMAGE_NAME_PREFIX_LEGTH));
	}

	/**
	 *
	 */
	protected String getHyperlinkURL(JRPrintHyperlink link)
	{
		return getHyperlinkURL(link, true);
	}
	
	/**
	 *
	 */
	protected String getHyperlinkURL(JRPrintHyperlink link, boolean isOnePagePerSheet)
	{
		String href = null;
		JRHyperlinkProducer customHandler = getHyperlinkProducer(link);
		if (customHandler == null)
		{
			switch(link.getHyperlinkType())
			{
				case REFERENCE :
				{
					if (link.getHyperlinkReference() != null)
					{
						href = link.getHyperlinkReference();
					}
					break;
				}
				case LOCAL_ANCHOR :
				{
					if (link.getHyperlinkAnchor() != null)
					{
						href = "#" + link.getHyperlinkAnchor();
					}
					break;
				}
				case LOCAL_PAGE :
				{
					if (link.getHyperlinkPage() != null)
					{
						href = "#" + JR_PAGE_ANCHOR_PREFIX + getReportIndex() + "_" + (isOnePagePerSheet ? link.getHyperlinkPage().toString() : "1");
					}
					break;
				}
				case REMOTE_ANCHOR :
				{
					if (
						link.getHyperlinkReference() != null &&
						link.getHyperlinkAnchor() != null
						)
					{
						href = link.getHyperlinkReference() + "#" + link.getHyperlinkAnchor();
					}
					break;
				}
				case REMOTE_PAGE :
				{
					if (
						link.getHyperlinkReference() != null &&
						link.getHyperlinkPage() != null
						)
					{
						href = link.getHyperlinkReference() + "#" + JR_PAGE_ANCHOR_PREFIX + "0_" + link.getHyperlinkPage().toString();
					}
					break;
				}
				case NONE :
				default :
				{
					break;
				}
			}
		}
		else
		{
			href = customHandler.getHyperlink(link);
		}

		return href;
	}

	/**
	 *
	 */
	protected RenderersCache getRenderersCache()
	{
		return renderersCache;
	}

	/**
	 *
	 */
	protected String getImagePath(
		Renderable renderer, 
		Dimension dimension, 
		Color backcolor, 
		JRExporterGridCell gridCell,
//		boolean isLazy,
		RenderersCache imageRenderersCache
		) throws JRException
	{
		String imagePath = null;
		
//		if (isLazy)  // honouring lazy images in ods/odt is unlike any other export except html and xml
//		{
//			// we do not cache imagePath for lazy images because the short location string is already cached inside the render itself
//			imagePath = RendererUtil.getResourceLocation(renderer);
//		}
//		else
//		{
			// by the time we get here, the resource renderer has already been loaded from cache
			
			if (
				renderer instanceof DataRenderable //we do not cache imagePath for non-data renderers because they render width different width/height each time
				&& rendererToImagePathMap.containsKey(renderer.getId())
				)
			{
				imagePath = rendererToImagePathMap.get(renderer.getId());
			}
			else
			{
				JRPrintElementIndex imageIndex = getElementIndex(gridCell);
				
				DataRenderable imageRenderer = 
					RendererUtil.getInstance(getKlexReportsContext()).getImageDataRenderable(
						imageRenderersCache,
						renderer, 
						dimension, 
						backcolor
						);

				byte[] data = imageRenderer.getData(getKlexReportsContext());
				
				if (ImageTypeEnum.WEBP == JRTypeSniffer.getImageTypeValue(data))
				{
					WrappingImageDataToGraphics2DRenderer graphics2DRenderer = new WrappingImageDataToGraphics2DRenderer(imageRenderer);
					data = new WrappingRenderToImageDataRenderer(graphics2DRenderer, graphics2DRenderer, null).getData(getKlexReportsContext());
				}
				
				oasisZip.addEntry(//FIXMEODT optimize with a different implementation of entry
					new FileBufferedZipEntry(
						"Pictures/" + DocumentBuilder.getImageName(imageIndex),
						data
						)
					);

				String imageName = DocumentBuilder.getImageName(imageIndex);
				imagePath = "Pictures/" + imageName;

				if (imageRenderer == renderer)
				{
					//cache imagePath only for true ImageRenderable instances because the wrapping ones render with different width/height each time
					rendererToImagePathMap.put(renderer.getId(), imagePath);
				}
			}
//		}

		return imagePath;
	}

	/**
	 *
	 */
	protected JRPrintElementIndex getElementIndex(JRExporterGridCell gridCell)
	{
		JRPrintElementIndex imageIndex =
			new JRPrintElementIndex(
					getReportIndex(),
					getPageIndex(),
					gridCell.getElementAddress()
					);
		return imageIndex;
	}

	/**
	 *
	 */
	public abstract JRStyledText getStyledText(JRPrintText text);

	/**
	 *
	 */
	public abstract Locale getTextLocale(JRPrintText text);

	/**
	 *
	 */
	public abstract String getInvalidCharReplacement();
	
	/**
	 * 
	 */
	protected abstract void insertPageAnchor(TableBuilder tableBuilder);

	/**
	 * 
	 */
	protected abstract JRHyperlinkProducer getHyperlinkProducer(JRPrintHyperlink link);

	/**
	 * 
	 */
	protected abstract KlexReportsContext getKlexReportsContext();

	/**
	 * 
	 */
	protected abstract int getReportIndex();

	/**
	 * 
	 */
	protected abstract int getPageIndex();

}