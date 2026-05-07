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

import java.util.Map;

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.JRQueryExecuter;
import net.sf.klexreports.engine.query.QueryExecutionContext;
import net.sf.klexreports.engine.query.SimpleQueryExecutionContext;

/**
 * JSON QL query executer factory.
 * <p/>
 * The factory creates {@link JsonQLQueryExecuter JsonQLQueryExecuter}
 * query executers.
 *
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JsonQLQueryExecuterFactory extends JsonQueryExecuterFactory
{
	
	public static final String JSONQL_QUERY_EXECUTER_NAME = "net.sf.klexreports.query.executer:JSONQL";
	
	@Override
	public JRQueryExecuter createQueryExecuter(
		KlexReportsContext klexReportsContext,
		JRDataset dataset, 
		Map<String, ? extends JRValueParameter> parameters
		) throws JRException
	{
		return createQueryExecuter(SimpleQueryExecutionContext.of(klexReportsContext), 
				dataset, parameters);
	}
	
	@Override
	public JRQueryExecuter createQueryExecuter(
		QueryExecutionContext context,
		JRDataset dataset, 
		Map<String, ? extends JRValueParameter> parameters
		) throws JRException
	{
		return new JsonQLQueryExecuter(context, dataset, parameters);
	}

	@Override
	public String getDesignation()
	{
		return JSONQL_QUERY_EXECUTER_NAME;
	}

}
