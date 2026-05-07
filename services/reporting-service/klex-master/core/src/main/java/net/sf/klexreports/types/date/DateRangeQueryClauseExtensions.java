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
package net.sf.klexreports.types.date;

import java.util.Date;

import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.query.JRJdbcQueryExecuter;
import net.sf.klexreports.engine.query.ParameterTypesClauseFunctionBundle;
import net.sf.klexreports.engine.query.StandardParameterTypesClauseFunction;
import net.sf.klexreports.engine.query.StandardSingleQueryParameterTypesClauseFunctionBundle;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.SingletonExtensionRegistry;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DateRangeQueryClauseExtensions implements ExtensionsRegistryFactory
{

	private static ExtensionsRegistry registry;

	static
	{
		StandardSingleQueryParameterTypesClauseFunctionBundle typesFunctions = 
				new StandardSingleQueryParameterTypesClauseFunctionBundle(JRJdbcQueryExecuter.CANONICAL_LANGUAGE);
		
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_EQUAL, 
				new StandardParameterTypesClauseFunction(DateRangeSQLEqualClause.instance(), DateRange.class));
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_NOTEQUAL, 
				new StandardParameterTypesClauseFunction(DateRangeSQLNotEqualClause.instance(), DateRange.class));
		
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_LESS, 
				new StandardParameterTypesClauseFunction(DateRangeSQLLessOrGreaterClause.instance(), DateRange.class));
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_GREATER, 
				new StandardParameterTypesClauseFunction(DateRangeSQLLessOrGreaterClause.instance(), DateRange.class));
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_LESS_OR_EQUAL, 
				new StandardParameterTypesClauseFunction(DateRangeSQLLessOrGreaterClause.instance(), DateRange.class));
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_GREATER_OR_EQUAL, 
				new StandardParameterTypesClauseFunction(DateRangeSQLLessOrGreaterClause.instance(), DateRange.class));
		
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_BETWEEN, 
				new StandardParameterTypesClauseFunction(DateRangeSQLBetweenClause.instance(), DateRange.class, Date.class));
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_BETWEEN_CLOSED, 
				new StandardParameterTypesClauseFunction(DateRangeSQLBetweenClause.instance(), DateRange.class, Date.class));
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_BETWEEN_LEFT_CLOSED, 
				new StandardParameterTypesClauseFunction(DateRangeSQLBetweenClause.instance(), DateRange.class, Date.class));
		typesFunctions.setFunctions(JRJdbcQueryExecuter.CLAUSE_ID_BETWEEN_RIGHT_CLOSED, 
				new StandardParameterTypesClauseFunction(DateRangeSQLBetweenClause.instance(), DateRange.class, Date.class));
		
		registry = new SingletonExtensionRegistry<ParameterTypesClauseFunctionBundle>(
				ParameterTypesClauseFunctionBundle.class, typesFunctions);
	}
	
	@Override
	public ExtensionsRegistry createRegistry(String registryId,
			JRPropertiesMap properties)
	{
		return registry;
	}

}
