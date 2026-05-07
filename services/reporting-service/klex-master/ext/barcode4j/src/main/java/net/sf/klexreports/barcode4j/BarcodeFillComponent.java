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

import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRStyle;
import net.sf.klexreports.engine.component.BaseFillComponent;
import net.sf.klexreports.engine.component.FillPrepareResult;
import net.sf.klexreports.engine.fill.JRTemplateImage;
import net.sf.klexreports.engine.fill.JRTemplatePrintImage;
import net.sf.klexreports.engine.type.EvaluationTimeEnum;
import net.sf.klexreports.engine.type.ScaleImageEnum;
import net.sf.klexreports.export.HtmlReportConfiguration;
import net.sf.klexreports.renderers.Renderable;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BarcodeFillComponent extends BaseFillComponent
{

	private final BarcodeComponent barcodeComponent;
	
	private final Map<JRStyle, JRTemplateImage> printTemplates = new HashMap<>();
	private Renderable renderable;
	
	
	public BarcodeFillComponent(BarcodeComponent barcodeComponent)
	{
		this.barcodeComponent = barcodeComponent;
	}

	public BarcodeFillComponent(BarcodeFillComponent barcode)
	{
		this.barcodeComponent = barcode.barcodeComponent;
	}
	
	@Override
	public void evaluate(byte evaluation) throws JRException
	{
		if (isEvaluateNow())
		{
			evaluateBarcode(evaluation);
		}
	}
	
	protected boolean isEvaluateNow()
	{
		return EvaluationTimeEnum.getValueOrDefault(barcodeComponent.getEvaluationTime()) == EvaluationTimeEnum.NOW;
	}

	protected void evaluateBarcode(byte evaluation)
	{
		BarcodeEvaluator evaluator = new BarcodeEvaluator(fillContext, evaluation);
		evaluator.evaluateBarcode();
		renderable = evaluator.getRenderable();
	}
	
	@Override
	public FillPrepareResult prepare(int availableHeight)
	{
		if (isEvaluateNow() && renderable == null)
		{
			return FillPrepareResult.NO_PRINT_NO_OVERFLOW;
		}
		
		return FillPrepareResult.PRINT_NO_STRETCH;
	}

	@Override
	public JRPrintElement fill()
	{
		JRTemplateImage templateImage = getTemplateImage();
		
		JRTemplatePrintImage image = new JRTemplatePrintImage(templateImage, printElementOriginator);
		JRComponentElement element = fillContext.getComponentElement();
		image.setUUID(element.getUUID());
		image.setX(element.getX());
		image.setY(fillContext.getElementPrintY());
		image.setWidth(element.getWidth());
		image.setHeight(element.getHeight());
		HtmlReportConfiguration.forceEmbedImage(fillContext.getFiller().getPropertiesUtil(), 
				fillContext.getComponentElement(), image);
		
		if (isEvaluateNow())
		{
			setBarcodeImage(image);
		}
		else
		{
			fillContext.registerDelayedEvaluation(image, 
					barcodeComponent.getEvaluationTime(), 
					barcodeComponent.getEvaluationGroup());
		}
		
		return image;
	}
	
	protected JRTemplateImage getTemplateImage()
	{
		JRStyle elementStyle = fillContext.getElementStyle();
		JRTemplateImage templateImage = printTemplates.get(elementStyle);
		if (templateImage == null)
		{
			templateImage = new JRTemplateImage(
					fillContext.getElementOrigin(), 
					fillContext.getDefaultStyleProvider());
			templateImage.setElement(fillContext.getComponentElement());
			templateImage.setStyle(elementStyle);//already set by setElement, but keeping for safety
			templateImage.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
			templateImage.setUsingCache(false);

			templateImage = deduplicate(templateImage);
			printTemplates.put(elementStyle, templateImage);
		}
		return templateImage;
	}
	
	protected void setBarcodeImage(JRTemplatePrintImage image)
	{
		if (renderable != null)
		{
			image.setRenderer(renderable);
		}
	}

	@Override
	public void evaluateDelayedElement(JRPrintElement element, byte evaluation)
			throws JRException
	{
		evaluateBarcode(evaluation);
		setBarcodeImage((JRTemplatePrintImage) element);
	}

}
