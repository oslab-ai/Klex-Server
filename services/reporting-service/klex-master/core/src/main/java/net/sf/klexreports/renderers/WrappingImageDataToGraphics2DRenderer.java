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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.lang.ref.SoftReference;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.ExifOrientationEnum;
import net.sf.klexreports.engine.util.ImageUtil;
import net.sf.klexreports.engine.util.JRImageLoader;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class WrappingImageDataToGraphics2DRenderer extends AbstractRenderer implements DataRenderable, Graphics2DRenderable, DimensionRenderable
{
	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	/**
	 *
	 */
	private final DataRenderable dataRenderer;

	/**
	 *
	 */
	private transient SoftReference<Image> awtImageRef;


	/**
	 *
	 */
	public WrappingImageDataToGraphics2DRenderer(DataRenderable dataRenderer) throws JRException
	{
		this.dataRenderer = dataRenderer;
	}


	/**
	 *
	 */
	protected Image getImage(KlexReportsContext klexReportsContext) throws JRException
	{
		if (awtImageRef == null || awtImageRef.get() == null)
		{
			Image awtImage = JRImageLoader.getInstance(klexReportsContext).loadAwtImageFromBytes(getData(klexReportsContext));
			awtImageRef = new SoftReference<>(awtImage);
		}
		return awtImageRef.get();
	}


	@Override
	public Dimension2D getDimension(KlexReportsContext klexReportsContext) throws JRException
	{
		Image img = getImage(klexReportsContext);
		return new Dimension(img.getWidth(null), img.getHeight(null));
	}


	@Override
	public byte[] getData(KlexReportsContext klexReportsContext)
			throws JRException
	{
		return dataRenderer.getData(klexReportsContext);
	}


	@Override
	public void render(KlexReportsContext klexReportsContext, Graphics2D grx, Rectangle2D rectangle) throws JRException
	{
		Image img = getImage(klexReportsContext);
		
		int translateX = 0;
		int translateY = 0;
		int renderWidth = (int)rectangle.getWidth();
		int renderHeight = (int)rectangle.getHeight();
		double angle = 0;
		
		ExifOrientationEnum exifOrientation = ImageUtil.getExifOrientation(getData(klexReportsContext));
		switch (exifOrientation)
		{
			case UPSIDE_DOWN :
			{
				translateX = (int)rectangle.getWidth();
				translateY = (int)rectangle.getHeight();
				angle = Math.PI;
				break;
			}
			case RIGHT :
			{
				translateX = (int)rectangle.getWidth();
				translateY = 0;
				renderWidth = (int)rectangle.getHeight();
				renderHeight = (int)rectangle.getWidth();
				angle = Math.PI / 2;
				break;
			}
			case LEFT :
			{
				translateX = 0;
				translateY = (int)rectangle.getHeight();
				renderWidth = (int)rectangle.getHeight();
				renderHeight = (int)rectangle.getWidth();
				angle = - Math.PI / 2;
				break;
			}
			case NORMAL :
			default :
			{
			}
		}
		
		AffineTransform oldTransform = grx.getTransform();

		grx.translate(
			(int)rectangle.getX() + translateX, 
			(int)rectangle.getY() + translateY
			);
		grx.rotate(angle);
		
		try
		{
			grx.drawImage(
				img, 
				0, 
				0, 
				renderWidth, 
				renderHeight, 
				null
				);
		}
		finally
		{
			grx.setTransform(oldTransform);
		}
	}
}
