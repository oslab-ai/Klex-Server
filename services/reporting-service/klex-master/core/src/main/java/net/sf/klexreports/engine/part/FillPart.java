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
package net.sf.klexreports.engine.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRPart;
import net.sf.klexreports.engine.JRPropertiesHolder;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertyExpression;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.PrintPart;
import net.sf.klexreports.engine.fill.JRFillExpressionEvaluator;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;
import net.sf.klexreports.engine.fill.PartReportFiller;
import net.sf.klexreports.engine.util.ElementalPropertiesHolder;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class FillPart
{

	private JRPart reportPart;
	private JRFillExpressionEvaluator expressionEvaluator;
	private PartReportFiller reportFiller;
	
	private JRPropertiesHolder staticPartProperties;
	private List<JRPropertyExpression> propertyExpressions;
	private JRPropertiesHolder printPartProperties;
	private PartFillComponent fillComponent;
	private String partName;

	public FillPart(JRPart part, JRFillObjectFactory fillFactory)
	{
		this.reportPart = part;
		this.expressionEvaluator = fillFactory.getExpressionEvaluator();
		reportFiller = (PartReportFiller) fillFactory.getReportFiller();//FIXMEBOOK
		
		PartComponent component = part.getComponent();
		
		KlexReportsContext klexReportsContext = fillFactory.getReportFiller().getKlexReportsContext();
		
		staticPartProperties = new ElementalPropertiesHolder();
		JRPropertiesUtil.getInstance(klexReportsContext).transferProperties(part, staticPartProperties, 
				PrintPart.PROPERTIES_TRANSFER_PREFIX);
		
		JRPropertyExpression[] partPropertyExpressions = part.getPropertyExpressions();
		propertyExpressions = partPropertyExpressions == null ? new ArrayList<>(0)
			: new ArrayList<>(Arrays.asList(partPropertyExpressions));
		
		PartComponentsEnvironment partsEnv = PartComponentsEnvironment.getInstance(klexReportsContext);
		PartComponentManager componentManager = partsEnv.getManager(component);
		PartComponentFillFactory componentFactory = componentManager.getComponentFillFactory(klexReportsContext);
		
		//fillFactory.trackDatasetRuns();FIXMEBOOK
		this.fillComponent = componentFactory.toFillComponent(component, fillFactory);
		fillComponent.initialize(new Context());
	}
	
	public void fill(byte evaluation, PartPrintOutput output) throws JRException
	{
		boolean toPrint = evaluatePrintWhenExpression(evaluation);
		if (!toPrint)
		{
			return;
		}
		
		evaluateProperties(evaluation);
		evaluatePartNameExpression(evaluation);
		fillComponent.evaluate(evaluation);
		fillComponent.fill(output);
	}

	protected boolean evaluatePrintWhenExpression(byte evaluation) throws JRException
	{
		JRExpression expression = reportPart.getPrintWhenExpression();
		boolean result;
		if (expression == null)
		{
			result = true;
		}
		else
		{
			Boolean expressionResult = (Boolean) expressionEvaluator.evaluate(expression, evaluation);
			result = expressionResult != null && expressionResult;
		}
		return result;
	}
	
	protected void evaluateProperties(byte evaluation) throws JRException
	{
		JRPropertiesMap dynamicProperties = new JRPropertiesMap();
		for (JRPropertyExpression prop : propertyExpressions)
		{
			String value = (String) expressionEvaluator.evaluate(prop.getValueExpression(), evaluation);
			dynamicProperties.setProperty(prop.getName(), value);
		}
		
		if (dynamicProperties.isEmpty())
		{
			printPartProperties = staticPartProperties;
		}
		else
		{
			JRPropertiesMap props = new JRPropertiesMap();
			props.setBaseProperties(staticPartProperties.getPropertiesMap());
			printPartProperties = new ElementalPropertiesHolder(props);
			JRPropertiesUtil.getInstance(reportFiller.getKlexReportsContext()).transferProperties(
					dynamicProperties, printPartProperties, PrintPart.PROPERTIES_TRANSFER_PREFIX);
		}
	}

	protected void evaluatePartNameExpression(byte evaluation) throws JRException
	{
		JRExpression expression = reportPart.getPartNameExpression();
		partName = expression == null ? null : (String) expressionEvaluator.evaluate(expression, evaluation);
	}

	public PartEvaluationTime getEvaluationTime()
	{
		PartEvaluationTime evaluationTime = reportPart.getEvaluationTime();
		return evaluationTime == null || evaluationTime.getEvaluationTimeType() == null ? StandardPartEvaluationTime.EVALUATION_NOW : evaluationTime;
	}

	public String getPartName()
	{
		return partName;
	}
	
	public JRPropertiesHolder getPrintPartProperties()
	{
		return printPartProperties;
	}

	protected class Context implements PartFillContext
	{
		@Override
		public JRPart getPart()
		{
			return reportPart;
		}

		@Override
		public FillPart getFillPart()
		{
			return FillPart.this;
		}

		@Override
		public PartReportFiller getFiller()
		{
			return reportFiller;
		}

		@Override
		public Object evaluate(JRExpression expression, byte evaluation) throws JRException
		{
			return reportFiller.evaluateExpression(expression, evaluation);
		}
	}
}
