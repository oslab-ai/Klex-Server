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
package net.sf.klexreports.javascript;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Script;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.fill.JREvaluator;
import net.sf.klexreports.engine.fill.JRFillField;
import net.sf.klexreports.engine.fill.JRFillParameter;
import net.sf.klexreports.engine.fill.JRFillVariable;
import net.sf.klexreports.engine.fill.KlexReportsContextAware;
import net.sf.klexreports.functions.FunctionsUtil;
import net.sf.klexreports.javascript.JavaScriptCompiledData.ExpressionIndexes;

/**
 * JavaScript expression evaluator that uses Java bytecode compiled by {@link JavaScriptClassCompiler}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JavaScriptCompiledEvaluator extends JREvaluator implements KlexReportsContextAware
{

	private static final Log log = LogFactory.getLog(JavaScriptCompiledEvaluator.class);

	protected static final String EXPRESSION_ID_VAR = "_jreid";
	
	private static final ReferenceMap<String, JavaScriptClassLoader> scriptClassLoaders = 
		new ReferenceMap<>(
			ReferenceMap.ReferenceStrength.HARD, ReferenceMap.ReferenceStrength.SOFT
			);
	
	protected static JavaScriptClassLoader getScriptClassLoader(String unitName)
	{
		JavaScriptClassLoader loader;
		boolean created = false;
		synchronized (scriptClassLoaders)
		{
			loader = scriptClassLoaders.get(unitName);
			if (loader == null)
			{
				loader = new JavaScriptClassLoader();
				scriptClassLoaders.put(unitName, loader);
				created = true;
			}
		}
		
		if (created && log.isDebugEnabled())
		{
			log.debug("created script class loader " + loader + " for " + unitName);
		}
		return loader;
	}
	
	private final KlexReportsContext klexReportsContext;
	private final String unitName;
	private final JavaScriptCompiledData compiledData;
	private FunctionsUtil functionsUtil;
	private JavaScriptEvaluatorScope evaluatorScope;
	
	private final Map<Integer, Script> scripts = new HashMap<>();


	/**
	 * Create a JavaScript expression evaluator.
	 * 
	 * @param klexReportsContext 
	 * @param unitName 
	 * @param compiledData the report compile data
	 */
	public JavaScriptCompiledEvaluator(KlexReportsContext klexReportsContext, String unitName, JavaScriptCompiledData compiledData)
	{
		this.klexReportsContext = klexReportsContext;
		this.unitName = unitName;
		this.compiledData = compiledData;
	}
	
	@Override
	public void setKlexReportsContext(KlexReportsContext context)
	{
		this.functionsUtil = FunctionsUtil.getInstance(context);
	}

	@Override
	protected void customizedInit(
			Map<String, JRFillParameter> parametersMap, 
			Map<String, JRFillField> fieldsMap,
			Map<String, JRFillVariable> variablesMap
			) throws JRException
	{
		evaluatorScope = new JavaScriptEvaluatorScope(klexReportsContext, this, functionsUtil);
		evaluatorScope.init(parametersMap, fieldsMap, variablesMap);
	}
	
	@Override
	protected Object evaluate(int id) throws Throwable //NOSONAR
	{
		JavaScriptCompiledData.ExpressionIndexes expression = getExpression(id);
		return evaluateExpression(expression.getDefaultExpressionIndex());
	}

	@Override
	protected Object evaluateEstimated(int id) throws Throwable //NOSONAR
	{
		JavaScriptCompiledData.ExpressionIndexes expression = getExpression(id);
		return evaluateExpression(expression.getEstimatedExpressionIndex());
	}

	@Override
	protected Object evaluateOld(int id) throws Throwable //NOSONAR
	{
		JavaScriptCompiledData.ExpressionIndexes expression = getExpression(id);
		return evaluateExpression(expression.getOldExpressionIndex());
	}

	protected ExpressionIndexes getExpression(int id)
	{
		return compiledData.getExpression(id);
	}
	
	protected Object evaluateExpression(int expressionIndex)
	{
		int scriptIndex = JavaScriptCompiledData.scriptIndex(expressionIndex);
		Script script = scripts.get(scriptIndex);
		if (script == null)
		{
			if (log.isTraceEnabled())
			{
				log.trace("creating script for expression index " + expressionIndex
						+ ", script index " + scriptIndex);
			}
			
			JavaScriptClassLoader scriptClassLoader = getScriptClassLoader(unitName);
			script = scriptClassLoader.createScript(scriptIndex, compiledData);
			scripts.put(scriptIndex, script);
		}
		
		int expressionId = JavaScriptCompiledData.expressionId(expressionIndex);
		evaluatorScope.setScopeVariable(EXPRESSION_ID_VAR, expressionId);
		Object value = evaluatorScope.evaluateExpression(script);
		if (log.isTraceEnabled())
		{
			log.trace("expression with index " + expressionIndex + ", id " + expressionId 
					+ " evaluated to " + value);
		}
		return value;
	}

}
