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
package net.sf.klexreports.engine.fill;

import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRExpression;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BuiltinFillExpressionEvaluatorDecorator implements
		JRFillExpressionEvaluator
{

	private final JRFillExpressionEvaluator decorated;
	private final DatasetExpressionEvaluator datasetAdapter;
	private final Map<JRExpression, BuiltinExpressionEvaluator> builtinEvaluators;
	
	public BuiltinFillExpressionEvaluatorDecorator(JRFillExpressionEvaluator decorated,
			Map<JRExpression, BuiltinExpressionEvaluator> builtinEvaluators)
	{
		this.decorated = decorated;
		this.datasetAdapter = new FillExpressionEvaluatorDatasetAdapter(decorated);
		this.builtinEvaluators = builtinEvaluators;
	}

	@Override
	public Object evaluate(JRExpression expression, byte evaluationType)
			throws JRException
	{
		BuiltinExpressionEvaluator builtinEvaluator = builtinEvaluators.get(expression);
		if (builtinEvaluator != null)
		{
			return builtinEvaluator.evaluate(datasetAdapter);
		}
		
		return decorated.evaluate(expression, evaluationType);
	}

	@Override
	public JRFillDataset getFillDataset()
	{
		return decorated.getFillDataset();
	}

}
