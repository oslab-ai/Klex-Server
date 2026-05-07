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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.base.JRBasePrintImage;
import net.sf.klexreports.engine.component.ComponentDesignConverter;
import net.sf.klexreports.engine.convert.ReportConverter;
import net.sf.klexreports.engine.type.ScaleImageEnum;
import net.sf.klexreports.renderers.Renderable;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BarcodeDesignConverter implements ComponentDesignConverter
{

	private static final Log log = LogFactory.getLog(BarcodeDesignConverter.class);
	
	@Override
	public JRPrintElement convert(ReportConverter reportConverter,
			JRComponentElement element)
	{
		JRBasePrintImage printImage = new JRBasePrintImage(
				reportConverter.getDefaultStyleProvider());
		reportConverter.copyBaseAttributes(element, printImage);
		printImage.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
		
		Renderable barcodeImage = evaluateBarcode(reportConverter, element);
		printImage.setRenderer(barcodeImage);
		
		return printImage;
	}

	protected Renderable evaluateBarcode(ReportConverter reportConverter,
			JRComponentElement element)
	{
		try
		{
			BarcodeDesignEvaluator evaluator = 
				new BarcodeDesignEvaluator(
					reportConverter.getKlexReportsContext(),
					element, 
					reportConverter.getDefaultStyleProvider()
					);
			return evaluator.evaluateImage();
		}
		catch (Exception e)
		{
			if (log.isWarnEnabled())
			{
				log.warn("Failed to create barcode preview", e);
			}
			
			return null;
		}
	}

}
