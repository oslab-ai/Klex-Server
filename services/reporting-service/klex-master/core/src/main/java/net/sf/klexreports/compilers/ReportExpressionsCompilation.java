/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.compilers;

import java.util.List;
import java.util.Map;

import net.sf.klexreports.engine.JRExpression;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ReportExpressionsCompilation
{
	
	private final List<JRExpression> sourceExpressions;
	private final Map<Integer, DirectExpressionEvaluation> directEvaluations;
	
	public ReportExpressionsCompilation(List<JRExpression> sourceExpressions,
			Map<Integer, DirectExpressionEvaluation> directEvaluations)
	{
		this.sourceExpressions = sourceExpressions;
		this.directEvaluations = directEvaluations;
	}

	public List<JRExpression> getSourceExpressions()
	{
		return sourceExpressions;
	}

	public Map<Integer, DirectExpressionEvaluation> getDirectEvaluations()
	{
		return directEvaluations;
	}

}
