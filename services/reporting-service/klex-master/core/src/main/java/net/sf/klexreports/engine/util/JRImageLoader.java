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
package net.sf.klexreports.engine.util;

import java.awt.Image;
import java.lang.reflect.InvocationTargetException;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.type.ImageTypeEnum;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class JRImageLoader
{


	/**
	 * Configuration property specifying the name of the class implementing the {@link JRImageReader} interface
	 * to be used by the engine. If not set, the engine will try to an image reader implementation that corresponds to the JVM version.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_2_0_5
			)
	public static final String PROPERTY_IMAGE_READER = JRPropertiesUtil.PROPERTY_PREFIX + "image.reader";

	/**
	 * Configuration property specifying the name of the class implementing the {@link JRImageEncoder} interface
	 * to be used by the engine. If not set, the engine will try to an image encoder implementation that corresponds to the JVM version.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_2_0_5
			)
	public static final String PROPERTY_IMAGE_ENCODER = JRPropertiesUtil.PROPERTY_PREFIX + "image.encoder";

	public static final String PIXEL_IMAGE_RESOURCE = "net/sf/klexreports/engine/images/white-transparent-pixel.png";
	public static final String NO_IMAGE_RESOURCE = "net/sf/klexreports/engine/images/image-16.png";
	public static final String SUBREPORT_IMAGE_RESOURCE = "net/sf/klexreports/engine/images/subreport-16.png";
	public static final String CHART_IMAGE_RESOURCE = "net/sf/klexreports/engine/images/chart-16.png";
	public static final String CROSSTAB_IMAGE_RESOURCE = "net/sf/klexreports/engine/images/crosstab-16.png";
	public static final String COMPONENT_IMAGE_RESOURCE = "net/sf/klexreports/engine/images/component-16.png";

	/**
	 *
	 */
	private JRImageReader imageReader;
	private JRImageEncoder imageEncoder;
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private JRImageLoader(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
		
		init();
	}
	
	
	/**
	 *
	 */
	public static JRImageLoader getInstance(KlexReportsContext klexReportsContext)
	{
		return new JRImageLoader(klexReportsContext);
	}
	
	
	private void init()
	{
		String readerClassName = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(PROPERTY_IMAGE_READER);
		if (readerClassName == null)
		{
			imageReader = new JRJdk14ImageReader();
		}
		else
		{
			try 
			{
				Class<?> clazz = JRClassLoader.loadClassForRealName(readerClassName);	
				imageReader = (JRImageReader) clazz.getDeclaredConstructor().newInstance();
			}
			catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException 
				| IllegalAccessException | InstantiationException e)
			{
				throw new JRRuntimeException(e);
			}
		}


		String encoderClassName = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(PROPERTY_IMAGE_ENCODER);
		if (encoderClassName == null)
		{
			imageEncoder = new JRJdk14ImageEncoder();
		}
		else
		{
			try 
			{
				Class<?> clazz = JRClassLoader.loadClassForRealName(encoderClassName);	
				imageEncoder = (JRImageEncoder) clazz.getDeclaredConstructor().newInstance();
			}
			catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException 
				| IllegalAccessException | InstantiationException e)
			{
				throw new JRRuntimeException(e);
			}
		}
	}


	/**
	 * Encoding the image object using an image encoder that supports the supplied image type.
	 * 
	 * @param image the java.awt.Image object to encode
	 * @param imageType the type of the image as specified by one of the constants defined in the ImageTypeEnum
	 * @return the encoded image data
	 */
	public byte[] loadBytesFromAwtImage(Image image, ImageTypeEnum imageType) throws JRException
	{
		return imageEncoder.encode(image, imageType);
	}


	/**
	 *
	 */
	public Image loadAwtImageFromBytes(byte[] bytes) throws JRException
	{
		return imageReader.readImage(bytes);
	}
}
