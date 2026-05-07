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
package net.sf.klexreports.engine.analytics.dataset;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.analytics.data.MultiAxisDataSource;
import net.sf.klexreports.engine.fill.JRCalculator;
import net.sf.klexreports.engine.fill.JRExpressionEvalException;
import net.sf.klexreports.engine.fill.JRFillCloneFactory;
import net.sf.klexreports.engine.fill.JRFillElementDataset;
import net.sf.klexreports.engine.fill.JRFillExpressionEvaluator;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class FillMultiAxisDataset extends JRFillElementDataset
{
	public static final String EXCEPTION_MESSAGE_KEY_CANNOT_CREATE_BUCKETING_SERVICE = "engine.analytics.dataset.cannot.create.bucketing.service";
	
	private final KlexReportsContext klexReportsContext;
	private final MultiAxisData data;
	private final JRFillExpressionEvaluator expressionEvaluator;
	private MultiAxisDataService dataService;
	
	public FillMultiAxisDataset(MultiAxisData data, JRFillObjectFactory factory)
	{
		super(data.getDataset(), factory);
		
		this.klexReportsContext = factory.getFiller().getKlexReportsContext();
		this.data = data;
		this.expressionEvaluator = factory.getExpressionEvaluator();
		
		factory.registerElementDataset(this);
	}
	
	public FillMultiAxisDataset(FillMultiAxisDataset dataset, JRFillCloneFactory factory)
	{
		super(dataset, factory);
		
		this.klexReportsContext = dataset.klexReportsContext;
		this.data = dataset.data;
		this.expressionEvaluator = dataset.expressionEvaluator;
	}

	@Override
	protected void customInitialize()
	{
		if (dataService == null)
		{
			try
			{
				dataService = createService(JRExpression.EVALUATION_DEFAULT);
			}
			catch (JRException e)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_CANNOT_CREATE_BUCKETING_SERVICE,
						(Object[])null,
						e);
			}
		}
		else
		{
			dataService.clearData();
		}
	}

	protected MultiAxisDataService createService(byte evaluation) throws JRException
	{
		return new MultiAxisDataService(klexReportsContext, expressionEvaluator, data, evaluation);
	}

	@Override
	protected void customEvaluate(JRCalculator calculator)
			throws JRExpressionEvalException
	{
		dataService.evaluateRecord(calculator);
	}

	@Override
	protected void customIncrement()
	{
		dataService.addRecord();
	}

	public void evaluateData(byte evaluationType) throws JRException
	{
		evaluateDatasetRun(evaluationType);
	}

	public MultiAxisDataSource getDataSource() throws JRException
	{
		//a final increment is needed when incrementType=Group
		//FIXME perform the increment when the data set ends instead of here
		increment();
		
		return dataService.createDataSource();
	}
	
	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		// NOP
	}

}
