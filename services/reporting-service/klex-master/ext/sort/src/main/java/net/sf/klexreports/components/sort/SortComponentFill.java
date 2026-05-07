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
package net.sf.klexreports.components.sort;

import net.sf.klexreports.components.util.FilterTypesEnum;
import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.component.BaseFillComponent;
import net.sf.klexreports.engine.component.FillPrepareResult;
import net.sf.klexreports.engine.design.JRAbstractCompiler;
import net.sf.klexreports.engine.fill.JRFillCloneFactory;
import net.sf.klexreports.engine.fill.JRFillCloneable;
import net.sf.klexreports.engine.fill.JRFillDataset;
import net.sf.klexreports.engine.fill.JRFillField;
import net.sf.klexreports.engine.fill.JRFillVariable;
import net.sf.klexreports.engine.fill.JRTemplateGenericElement;
import net.sf.klexreports.engine.fill.JRTemplateGenericPrintElement;
import net.sf.klexreports.engine.type.EvaluationTimeEnum;
import net.sf.klexreports.engine.type.SortFieldTypeEnum;
import net.sf.klexreports.interactivity.sort.SortElement;
import net.sf.klexreports.interactivity.sort.SortElementUtils;

/**
 * 
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class SortComponentFill extends BaseFillComponent {

	private final SortComponent sortComponent;
	
	private	JRTemplateGenericElement template;
	
	private JRTemplateGenericPrintElement printElement;

	
	public SortComponentFill(SortComponent sortComponent)
	{
		this.sortComponent = sortComponent;
	}
	
	protected SortComponent getSortComponent()
	{
		return sortComponent;
	}
	
	protected boolean isEvaluateNow()
	{
		return sortComponent.getEvaluationTime() == EvaluationTimeEnum.NOW;
	}
	
	@Override
	public void evaluate(byte evaluation) throws JRException
	{
		if (isEvaluateNow())
		{
			evaluateSortComponent(evaluation);
		}
	}
	
	protected void evaluateSortComponent(byte evaluation) throws JRException 
	{
	}
	
	
	@Override
	public JRPrintElement fill()
	{
		printElement.setY(fillContext.getElementPrintY());
		return printElement;
	}

	@Override
	public FillPrepareResult prepare(int availableHeight)
	{
		FillPrepareResult result = null;
		
		JRComponentElement element = fillContext.getComponentElement();
		if (template == null) {
			template = new JRTemplateGenericElement(
					fillContext.getElementOrigin(), 
					fillContext.getDefaultStyleProvider(),
					SortElement.SORT_ELEMENT_TYPE);
		
			template.setMode(sortComponent.getContext().getComponentElement().getMode());
			template.setBackcolor(sortComponent.getContext().getComponentElement().getBackcolor());
			template.setForecolor(sortComponent.getContext().getComponentElement().getForecolor());
			
			template = deduplicate(template);
		}
		
		printElement = new JRTemplateGenericPrintElement(template, printElementOriginator);
		printElement.setUUID(element.getUUID());
		printElement.setX(element.getX());

		printElement.setWidth(element.getWidth());
		printElement.setHeight(element.getHeight());
		
		if (isEvaluateNow())
		{
			copy(printElement);
		}
		else
		{
			fillContext.registerDelayedEvaluation(printElement, 
					sortComponent.getEvaluationTime(), null);
		}
		
		result = FillPrepareResult.PRINT_NO_STRETCH;
		return result;
	}
	
	public JRFillCloneable createClone(JRFillCloneFactory factory)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void evaluateDelayedElement(JRPrintElement element, byte evaluation) throws JRException
	{
		evaluateSortComponent(evaluation);
		copy((JRGenericPrintElement) element);
	}

	protected void copy(JRGenericPrintElement printElement)
	{
		printElement.setParameterValue(SortElement.PARAMETER_SORT_COLUMN_NAME, sortComponent.getSortFieldName());
		printElement.setParameterValue(SortElement.PARAMETER_SORT_COLUMN_TYPE, sortComponent.getSortFieldType().getName());
		printElement.setParameterValue(SortElement.PARAMETER_SORT_HANDLER_COLOR, sortComponent.getHandlerColor());
		printElement.setParameterValue(SortElement.PARAMETER_SORT_HANDLER_FONT, sortComponent.getSymbolFont());
		
		if (sortComponent.getSymbolFont() != null ) {
			printElement.setParameterValue(SortElement.PARAMETER_SORT_HANDLER_FONT_SIZE, String.valueOf(sortComponent.getSymbolFont().getFontSize()));
		} 
		if (sortComponent.getHandlerHorizontalImageAlign() != null) 
		{
			printElement.setParameterValue(SortElement.PARAMETER_SORT_HANDLER_HORIZONTAL_ALIGN, sortComponent.getHandlerHorizontalImageAlign().getName());
		}
		if (sortComponent.getHandlerVerticalImageAlign() != null) 
		{
			printElement.setParameterValue(SortElement.PARAMETER_SORT_HANDLER_VERTICAL_ALIGN, sortComponent.getHandlerVerticalImageAlign().getName());
		}
		
		FilterTypesEnum filterType = getFilterType();
		if (filterType != null)
		{
			printElement.getPropertiesMap().setProperty(SortElement.PROPERTY_FILTER_TYPE, filterType.getName());
		}
		
		String datasetName = JRAbstractCompiler.getUnitName(
				fillContext.getFiller().getKlexReport(), fillContext.getFillDataset());
		printElement.getPropertiesMap().setProperty(SortElement.PROPERTY_DATASET_RUN, datasetName);
	}

	protected FilterTypesEnum getFilterType()
	{
		SortFieldTypeEnum type = sortComponent.getSortFieldType();
		String name = sortComponent.getSortFieldName();
		JRFillDataset dataset = fillContext.getFillDataset();
		
		FilterTypesEnum filterType = null;
		if (SortFieldTypeEnum.FIELD.equals(type))
		{
			JRFillField field = dataset.getFillField(name);
			filterType = SortElementUtils.getFilterType(field.getValueClass());
		}
		else if (SortFieldTypeEnum.VARIABLE.equals(type))
		{
			JRFillVariable variable = dataset.getFillVariable(name);
			filterType = SortElementUtils.getFilterType(variable.getValueClass());
		}
		return filterType;
	}
}
