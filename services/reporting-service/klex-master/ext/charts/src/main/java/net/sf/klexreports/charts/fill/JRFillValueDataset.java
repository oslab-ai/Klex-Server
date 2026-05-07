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
package net.sf.klexreports.charts.fill;

import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultValueDataset;

import net.sf.klexreports.charts.ChartsExpressionCollector;
import net.sf.klexreports.charts.JRChartDataset;
import net.sf.klexreports.charts.JRValueDataset;
import net.sf.klexreports.charts.design.ChartsVerifier;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.fill.JRCalculator;
import net.sf.klexreports.engine.fill.JRExpressionEvalException;

/**
 * @author Barry Klawans (bklawans@users.sourceforge.net)
 */
public class JRFillValueDataset extends JRFillChartDataset implements JRValueDataset
{

	private Number value;

	/**
	 *
	 */
	private DefaultValueDataset valueDataset = new DefaultValueDataset();


	/**
	 *
	 */
	public JRFillValueDataset(JRValueDataset valueDataset, ChartsFillObjectFactory factory)
	{
		super(valueDataset, factory.getParent());
	}

	@Override
	public JRExpression getValueExpression()
	{
		return ((JRValueDataset)parent).getValueExpression();
	}


	@Override
	protected void customInitialize()
	{
		valueDataset = new DefaultValueDataset();
	}

	@Override
	protected void customEvaluate(JRCalculator calculator) throws JRExpressionEvalException
	{
		value = (Number)calculator.evaluate(getValueExpression());
	}

	@Override
	protected void customIncrement()
	{
		valueDataset.setValue(value);
	}

	@Override
	public Dataset getCustomDataset()
	{
		return valueDataset;
	}

	@Override
	public Object getLabelGenerator()
	{
		return null;
	}

	@Override
	public byte getDatasetType() {
		return JRChartDataset.VALUE_DATASET;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		collector.collect(this);
	}

	@Override
	public void collectExpressions(ChartsExpressionCollector collector)
	{
		collector.collect(this);
	}

	@Override
	public void validate(ChartsVerifier verifier)
	{
		verifier.verify(this);
	}

}
