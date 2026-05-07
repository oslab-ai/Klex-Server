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
package net.sf.klexreports.jakarta.web;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.HtmlExporter;
import net.sf.klexreports.engine.type.ImageTypeEnum;
import net.sf.klexreports.engine.type.ModeEnum;
import net.sf.klexreports.engine.type.OnErrorTypeEnum;
import net.sf.klexreports.engine.util.JRImageLoader;
import net.sf.klexreports.engine.util.JRTypeSniffer;
import net.sf.klexreports.renderers.DataRenderable;
import net.sf.klexreports.renderers.Renderable;
import net.sf.klexreports.renderers.ResourceRenderer;
import net.sf.klexreports.renderers.util.RendererUtil;
import net.sf.klexreports.repo.RepositoryUtil;
import net.sf.klexreports.web.servlets.KlexPrintAccessor;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ImageWebResourceHandler implements WebResourceHandler
{
	public static final String EXCEPTION_MESSAGE_KEY_KLEXPRINT_NOT_FOUND = "web.util.klexprint.not.found";
	public static final String EXCEPTION_MESSAGE_KEY_REPORT_CONTEXT_NOT_FOUND = "web.util.report.context.not.found";
	
	/**
	 *
	 */
	public static final String REQUEST_PARAMETER_IMAGE_NAME = "image";
	
	private static final ImageWebResourceHandler INSTANCE = new ImageWebResourceHandler();
	
	public static ImageWebResourceHandler getInstance()
	{
		return INSTANCE;
	}
	
	private ImageWebResourceHandler()
	{
	}

			
	@Override
	public boolean handleResource(KlexReportsContext klexReportsContext, HttpServletRequest request, HttpServletResponse response)
	{
		String imageName = request.getParameter(REQUEST_PARAMETER_IMAGE_NAME);
		if (imageName == null)
		{
			return false;
		}

		byte[] imageData = null;
		String imageMimeType = null;

		if ("px".equals(imageName))
		{
			try
			{
				imageData = RepositoryUtil.getInstance(klexReportsContext).getBytesFromLocation(JRImageLoader.PIXEL_IMAGE_RESOURCE);
				imageMimeType = ImageTypeEnum.PNG.getMimeType();
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
		}
		else
		{
			WebReportContext webReportContext = WebReportContext.getInstance(request, false);
			
			if (webReportContext == null)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_REPORT_CONTEXT_NOT_FOUND,
						(Object[])null);
			}
			
			KlexPrintAccessor klexPrintAccessor = (KlexPrintAccessor) webReportContext.getParameterValue(
					KlexPrintAccessor.REPORT_CONTEXT_PARAMETER_KLEX_PRINT_ACCESSOR);
			if (klexPrintAccessor == null)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_KLEXPRINT_NOT_FOUND,
						(Object[])null);
			}
			
			List<KlexPrint> klexPrintList = Collections.singletonList(klexPrintAccessor.getKlexPrint());
			
			JRPrintImage image = HtmlExporter.getImage(klexPrintList, imageName);
			
			Renderable renderer = image.getRenderer();
			
			Dimension dimension = new Dimension(image.getWidth(), image.getHeight());
			Color backcolor = ModeEnum.OPAQUE == image.getMode() ? image.getBackcolor() : null;

			RendererUtil rendererUtil = RendererUtil.getInstance(klexReportsContext);
			
			try
			{
				imageData = process(klexReportsContext, renderer, dimension, backcolor);
			}
			catch (Exception e)
			{
				try
				{
					Renderable onErrorRenderer = rendererUtil.handleImageError(e, image.getOnErrorType());
					if (onErrorRenderer != null)
					{
						imageData = process(klexReportsContext, onErrorRenderer, dimension, backcolor);
					}
				}
				catch (JRException je)
				{
					throw new JRRuntimeException(je);
				}
			}
			
			imageMimeType =
				RendererUtil.getInstance(klexReportsContext).isSvgData(imageData)
				? RendererUtil.SVG_MIME_TYPE
				: JRTypeSniffer.getImageTypeValue(imageData).getMimeType();
		}

		if (imageData != null && imageData.length > 0)
		{
			if (imageMimeType != null) 
			{
				response.setHeader("Content-Type", imageMimeType);
			}
			response.setContentLength(imageData.length);
			
			ServletOutputStream outputStream = null;
			try
			{
				outputStream = response.getOutputStream();
				outputStream.write(imageData, 0, imageData.length);
				outputStream.flush();
			}
			catch (IOException e)
			{
				throw new JRRuntimeException(e);
			}
			finally
			{
				if (outputStream != null)
				{
					try
					{
						outputStream.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}
		
		return true;
	}

	
	protected byte[] process(
		KlexReportsContext klexReportsContext,
		Renderable renderer,
		Dimension dimension,
		Color backcolor
		) throws JRException
	{
		RendererUtil rendererUtil = RendererUtil.getInstance(klexReportsContext);
		
		if (renderer instanceof ResourceRenderer)
		{
			renderer = //hard to use a cache here and it would be just for some icon type of images, if any 
				rendererUtil.getNonLazyRenderable(
					((ResourceRenderer)renderer).getResourceLocation(), 
					OnErrorTypeEnum.ERROR
					);
		}
		
		DataRenderable dataRenderer = 
			rendererUtil.getDataRenderable(
				renderer,
				dimension,
				backcolor
				);

		return dataRenderer.getData(klexReportsContext);
	}
}
