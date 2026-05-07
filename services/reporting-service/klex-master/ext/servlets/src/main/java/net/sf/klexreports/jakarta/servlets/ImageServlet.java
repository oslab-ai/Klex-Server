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
package net.sf.klexreports.jakarta.servlets;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.KlexPrint;
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


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ImageServlet extends BaseHttpServlet
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;


	/**
	 *
	 */
	public static final String IMAGE_NAME_REQUEST_PARAMETER = "image";

			
	@Override
	public void service(
		HttpServletRequest request,
		HttpServletResponse response
		) throws IOException, ServletException
	{
		byte[] imageData = null;
		String imageMimeType = null;

		String imageName = request.getParameter(IMAGE_NAME_REQUEST_PARAMETER);
		if ("px".equals(imageName))
		{
			try
			{
				imageData = RepositoryUtil.getInstance(getKlexReportsContext()).getBytesFromLocation(JRImageLoader.PIXEL_IMAGE_RESOURCE);
				imageMimeType = ImageTypeEnum.PNG.getMimeType();
			}
			catch (JRException e)
			{
				throw new ServletException(e);
			}
		}
		else
		{
			List<KlexPrint> klexPrintList = BaseHttpServlet.getKlexPrintList(request);
			
			if (klexPrintList == null)
			{
				throw new ServletException("No KlexPrint documents found on the HTTP session.");
			}
			
			JRPrintImage image = HtmlExporter.getImage(klexPrintList, imageName);
			
			Renderable renderer = image.getRenderer();
			
			Dimension dimension = new Dimension(image.getWidth(), image.getHeight());
			Color backcolor = ModeEnum.OPAQUE == image.getMode() ? image.getBackcolor() : null;
			
			RendererUtil rendererUtil = RendererUtil.getInstance(getKlexReportsContext());
			
			try
			{
				imageData = process(renderer, dimension, backcolor);
			}
			catch (Exception e)
			{
				try
				{
					Renderable onErrorRenderer = rendererUtil.handleImageError(e, image.getOnErrorType());
					if (onErrorRenderer != null)
					{
						imageData = process(onErrorRenderer, dimension, backcolor);
					}
				}
				catch (Exception ex)
				{
					throw new ServletException(ex);
				}
			}
			
			imageMimeType = 
				rendererUtil.isSvgData(imageData)
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
			ServletOutputStream outputStream = response.getOutputStream();
			outputStream.write(imageData, 0, imageData.length);
			outputStream.flush();
			outputStream.close();
		}
	}

	
	protected byte[] process(
		Renderable renderer,
		Dimension dimension,
		Color backcolor
		) throws JRException
	{
		RendererUtil rendererUtil = RendererUtil.getInstance(getKlexReportsContext());
		
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
		
		return dataRenderer.getData(getKlexReportsContext());
	}
}
