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
package net.sf.klexreports.json.query;

import java.io.InputStream;
import java.util.Map;

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.data.RewindableDataSourceProvider;
import net.sf.klexreports.engine.data.TextDataSourceAttributes;
import net.sf.klexreports.engine.query.QueryExecutionContext;
import net.sf.klexreports.engine.query.SimpleQueryExecutionContext;
import net.sf.klexreports.engine.util.JRStringUtil;
import net.sf.klexreports.json.data.JsonQLDataSource;
import net.sf.klexreports.json.data.JsonQLDataSourceProvider;

/**
 * Simple JSON query executer implementation.
 * 
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JsonQLQueryExecuter extends AbstractJsonQueryExecuter<JsonQLDataSource>
{
	public static final String CANONICAL_LANGUAGE = "JSONQL";

	/**
	 *
	 */
	public JsonQLQueryExecuter(
		KlexReportsContext klexReportsContext,
		JRDataset dataset, 
		Map<String, ? extends JRValueParameter> parametersMap
		)
	{
		this(SimpleQueryExecutionContext.of(klexReportsContext), 
				dataset, parametersMap);
	}
	
	public JsonQLQueryExecuter(
		QueryExecutionContext context,
		JRDataset dataset, 
		Map<String, ? extends JRValueParameter> parametersMap
		)
	{
		super(context, dataset, parametersMap);
	}

	@Override
	protected String getCanonicalQueryLanguage()
	{
		return CANONICAL_LANGUAGE;
	}

	@Override
	protected String getParameterReplacement(String parameterName)
	{
		Object parameterValue = getParameterValue(parameterName);

		if (parameterValue == null) {
			return null;
		}

		Class<?> valueClass= parameterValue.getClass();

		if (Number.class.isAssignableFrom(valueClass) || Boolean.class.equals(valueClass)) {
			return String.valueOf(parameterValue);
		}

		StringBuilder sb = new StringBuilder("\"");
		sb.append(JRStringUtil.escapeJavaStringLiteral(String.valueOf(parameterValue)));
		sb.append("\"");

		return sb.toString();
	}

	@Override
	protected JsonQLDataSource getJsonDataInstance(InputStream jsonInputStream) throws JRException {
		return new JsonQLDataSource(jsonInputStream, getQueryString());
	}

	@Override
	protected JsonQLDataSource getJsonDataInstance(String jsonSource) throws JRException {
		return new JsonQLDataSource(getRepositoryContext(), jsonSource, getQueryString());
	}

	@Override
	protected RewindableDataSourceProvider<JsonQLDataSource> getJsonDataProviderInstance(String source, TextDataSourceAttributes textAttributes) {
		return new JsonQLDataSourceProvider(getKlexReportsContext(), source, getQueryString(), textAttributes);
	}
}
