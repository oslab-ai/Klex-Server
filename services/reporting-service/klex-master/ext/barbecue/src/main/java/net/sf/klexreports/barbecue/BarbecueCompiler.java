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
package net.sf.klexreports.barbecue;

import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentCompiler;
import net.sf.klexreports.engine.design.JRVerifier;
import net.sf.klexreports.engine.type.EvaluationTimeEnum;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BarbecueCompiler implements ComponentCompiler
{
	
	@Override
	public void collectExpressions(Component component, JRExpressionCollector collector)
	{
		BarbecueComponent barcode = (BarbecueComponent) component;
		collector.addExpression(barcode.getCodeExpression());
		collector.addExpression(barcode.getApplicationIdentifierExpression());
	}

	@Override
	public Component toCompiledComponent(Component component,
			JRBaseObjectFactory baseFactory)
	{
		BarbecueComponent barcode = (BarbecueComponent) component;
		return new StandardBarbecueComponent(barcode, baseFactory);
	}

	@Override
	public void verify(Component component, JRVerifier verifier)
	{
		BarbecueComponent barcode = (BarbecueComponent) component;
		
		String type = barcode.getType();
		if (type == null)
		{
			verifier.addBrokenRule("No barcode type set", barcode);
		}
		
		JRExpression codeExpression = barcode.getCodeExpression();
		if (codeExpression == null)
		{
			verifier.addBrokenRule("Barcode expression is null", barcode);
		}
		
		EvaluationTimeEnum evaluationTime = barcode.getEvaluationTime();
		if (evaluationTime == EvaluationTimeEnum.AUTO)
		{
			verifier.addBrokenRule("Auto evaluation time is not supported for barcodes", barcode);
		}
		else if (evaluationTime == EvaluationTimeEnum.GROUP)
		{
			String evaluationGroup = barcode.getEvaluationGroup();
			if (evaluationGroup == null || evaluationGroup.length() == 0)
			{
				verifier.addBrokenRule("No evaluation group set for barcode", barcode);
			}
			else if (!verifier.getReportDesign().getGroupsMap().containsKey(evaluationGroup))
			{
				verifier.addBrokenRule("Barcode evaluation group \"" 
						+ evaluationGroup + " not found", barcode);
			}
		}
	}

}
