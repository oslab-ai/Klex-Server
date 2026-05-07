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
package net.sf.klexreports.engine.design;

import java.util.List;
import java.util.Map;

import net.sf.klexreports.compilers.ReportSourceCompilation;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.JRField;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRVariable;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.DigestUtils;

/**
 * Expression evaluator source code generation information.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRSourceCompileTask
{
	private KlexDesign klexDesign;
	private String unitName;
	private JRExpressionCollector expressionCollector;
	private Map<String, ? extends JRParameter> parametersMap;
	private Map<String, JRField> fieldsMap;
	private Map<String, JRVariable> variablesMap;
	private JRVariable[] variables;
	private List<JRExpression> expressions;
	private boolean onlyDefaultEvaluation;
	
	private String compileName;
	
	protected JRSourceCompileTask(
			KlexDesign klexDesign, 
			String unitName, 
			JRExpressionCollector expressionCollector, 
			List<JRExpression> expressions,
			Map<String, ? extends JRParameter> parametersMap, 
			Map<String, JRField> fieldsMap, 
			Map<String, JRVariable> variablesMap, 
			JRVariable[] variables, 
			boolean onlyDefaultEvaluation
			)
	{
		this.klexDesign = klexDesign;
		this.unitName = unitName;
		this.expressionCollector = expressionCollector;
		this.parametersMap = parametersMap;
		this.fieldsMap = fieldsMap;
		this.variablesMap = variablesMap;
		this.variables = variables;
		this.expressions = expressions;
		this.onlyDefaultEvaluation = onlyDefaultEvaluation;
		
		this.compileName = computeCompileName();
	}
	
	public JRSourceCompileTask(
			KlexDesign klexDesign, 
			String unitName, 
			JRExpressionCollector expressionCollector,
			ReportSourceCompilation<?> sourceCompilation,
			boolean onlyDefaultEvaluation
			)
	{
		this(klexDesign, unitName, expressionCollector,
				sourceCompilation.getExpressions(),
				sourceCompilation.getParameters(),
				sourceCompilation.getFields(),
				sourceCompilation.getVariables(),
				sourceCompilation.getVariablesArray(),
				onlyDefaultEvaluation);
	}
	
	private String computeCompileName()
	{
		StringBuilder sourceText = new StringBuilder();
		if (parametersMap != null)
		{
			for (JRParameter parameter : parametersMap.values())
			{
				sourceText.append("p;");
				sourceText.append(parameter.getName());
				sourceText.append(";");
				sourceText.append(parameter.getValueClassName());
				sourceText.append(";");
			}
		}
		if (fieldsMap != null)
		{
			for (JRField field : fieldsMap.values())
			{
				sourceText.append("f;");
				sourceText.append(field.getName());
				sourceText.append(";");
				sourceText.append(field.getValueClassName());
				sourceText.append(";");
			}
		}
		if (variables != null)
		{
			for (JRVariable variable : variables)
			{
				sourceText.append("v;");
				sourceText.append(variable.getName());
				sourceText.append(";");
				sourceText.append(variable.getValueClassName());
				sourceText.append(";");
			}
		}
		if (expressions != null)
		{
			for (JRExpression expression : expressions)
			{
				sourceText.append("e;");
				sourceText.append(expressionCollector.getExpressionId(expression));
				sourceText.append(";");
				sourceText.append(expression.getText());
				sourceText.append(";");
			}
		}
		sourceText.append("onlyDefaultEvaluation;");
		sourceText.append(onlyDefaultEvaluation);
		
		String hash = DigestUtils.instance().sha256(sourceText.toString());
		return unitName + "_" + hash;
	}

	public List<JRExpression> getExpressions()
	{
		return expressions;
	}


	public Map<String, JRField> getFieldsMap()
	{
		return fieldsMap;
	}


	public KlexDesign getKlexDesign()
	{
		return klexDesign;
	}

	
	public String[] getImports()
	{
		return klexDesign.getImports();
	}
	
	
	public boolean isOnlyDefaultEvaluation()
	{
		return onlyDefaultEvaluation;
	}


	public Map<String, ? extends JRParameter> getParametersMap()
	{
		return parametersMap;
	}


	public String getUnitName()
	{
		return unitName;
	}

	public String getCompileName()
	{
		return compileName;
	}

	public JRVariable[] getVariables()
	{
		return variables;
	}


	public Map<String, JRVariable> getVariablesMap()
	{
		return variablesMap;
	}
	
	
	public Integer getExpressionId(JRExpression expression)
	{
		return expressionCollector.getExpressionId(expression);
	}
	
	
	public JRExpression getExpression(int expressionId)
	{
		return expressionCollector.getExpression(expressionId);
	}
	
	public KlexReportsContext getKlexReportsContext()
	{
		return expressionCollector.getKlexReportsContext();
	}
}
