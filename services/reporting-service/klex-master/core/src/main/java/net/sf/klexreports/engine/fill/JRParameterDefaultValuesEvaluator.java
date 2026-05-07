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

import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.repo.RepositoryContext;
import net.sf.klexreports.repo.SimpleRepositoryContext;


/**
 * Utility class to be used to evaluate parameter default value expressions for a report
 * without actually filling it.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public final class JRParameterDefaultValuesEvaluator
{

	/**
	 * Evaluates the default values for the parameters of a report.
	 * 
	 * @param report the report
	 * @param initialParameters initial parameter value map
	 * @return a map containing parameter values indexed by parameter names
	 * @throws JRException
	 */
	public static Map<String,Object> evaluateParameterDefaultValues(KlexReport report, Map<String,Object> initialParameters) throws JRException
	{
		return evaluateParameterDefaultValues(DefaultKlexReportsContext.getInstance(), report, initialParameters);
	}

	/**
	 * Evaluates the default values for the parameters of a report.
	 * 
	 * @param report the report
	 * @param initialParameters initial parameter value map
	 * @return a map containing parameter values indexed by parameter names
	 * @throws JRException
	 */
	public static Map<String,Object> evaluateParameterDefaultValues(KlexReportsContext klexReportsContext, KlexReport report, Map<String,Object> initialParameters) throws JRException
	{
		return evaluateParameterDefaultValues(SimpleRepositoryContext.of(klexReportsContext), report, initialParameters);
	}

	/**
	 * Evaluates the default values for the parameters of a report.
	 * 
	 * @param repositoryContext the repository context
	 * @param report the report
	 * @param initialParameters initial parameter value map
	 * @return a map containing parameter values indexed by parameter names
	 * @throws JRException
	 */
	//TODO use KlexReportSource instead of RepositoryContext?
	public static Map<String,Object> evaluateParameterDefaultValues(RepositoryContext repositoryContext, KlexReport report, Map<String,Object> initialParameters) throws JRException
	{
		Map<String,Object> parameterValues = new HashMap<>();
		DatasetExecution datasetExecution = new DatasetExecution(repositoryContext, report, initialParameters);
		datasetExecution.evaluateParameters((param, value) ->
		{
			if (!param.isSystemDefined())
			{
				parameterValues.put(param.getName(), value);
			}
		});
		return parameterValues;
	}
	

	private JRParameterDefaultValuesEvaluator()
	{
	}
}
