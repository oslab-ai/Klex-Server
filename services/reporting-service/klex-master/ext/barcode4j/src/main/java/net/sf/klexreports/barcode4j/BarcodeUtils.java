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
package net.sf.klexreports.barcode4j;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesHolder;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRSingletonCache;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public final class BarcodeUtils
{
	protected static JRSingletonCache<BarcodeImageProducer> imageProducerCache = 
		new JRSingletonCache<>(BarcodeImageProducer.class);

	protected static JRSingletonCache<QRCodeImageProducer> qrCodeProducerCache = 
			new JRSingletonCache<>(QRCodeImageProducer.class);

	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private BarcodeUtils(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	public static BarcodeUtils getInstance(KlexReportsContext klexReportsContext)
	{
		return new BarcodeUtils(klexReportsContext);
	}
	
	
	/**
	 * 
	 */
	public BarcodeImageProducer getProducer(JRPropertiesHolder propertiesHolder)
	{
		String producerProperty = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(propertiesHolder, 
				BarcodeImageProducer.PROPERTY_IMAGE_PRODUCER);
		
		String producerClass = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(propertiesHolder, 
				BarcodeImageProducer.PROPERTY_PREFIX_IMAGE_PRODUCER + producerProperty);
		if (producerClass == null)
		{
			producerClass = producerProperty;
		}
		
		try
		{
			return imageProducerCache.getCachedInstance(producerClass);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	
	/**
	 * 
	 */
	public QRCodeImageProducer getQRCodeProducer(JRPropertiesHolder propertiesHolder)
	{
		String producerProperty = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(propertiesHolder, 
				BarcodeImageProducer.PROPERTY_IMAGE_PRODUCER);
		
		String producerClass = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(propertiesHolder, 
				QRCodeImageProducer.PROPERTY_PREFIX_QRCODE_PRODUCER + producerProperty);
		if (producerClass == null)
		{
			producerClass = producerProperty;
		}
		
		try
		{
			return qrCodeProducerCache.getCachedInstance(producerClass);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	
	public static boolean isVertical(Barcode4jComponent barcode)
	{
		OrientationEnum orientation = barcode.getOrientation();
		return orientation == OrientationEnum.LEFT
				|| orientation == OrientationEnum.RIGHT;
	}
}
