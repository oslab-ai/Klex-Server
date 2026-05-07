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

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.query.ClauseFunctionParameterHandler;
import net.sf.klexreports.engine.query.DefaultClauseFunctionParameterHandler;
import net.sf.klexreports.engine.query.JRQueryClauseContext;
import net.sf.klexreports.engine.query.SQLBetweenBaseClause;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DateRangeSQLBetweenClause extends SQLBetweenBaseClause
{

	protected static final DateRangeSQLBetweenClause INSTANCE = new DateRangeSQLBetweenClause();
	public static final String EXCEPTION_MESSAGE_KEY_UNSUPPORTED_PARAMETER_TYPE = "date.range.unsupported.parameter.type";
	
	/**
	 * Returns the singleton function instance.
	 * 
	 * @return the singleton function instance
	 */
	public static DateRangeSQLBetweenClause instance()
	{
		return INSTANCE;
	}
	
	protected DateRangeSQLBetweenClause()
	{
	}

	@Override
	protected ClauseFunctionParameterHandler createParameterHandler(JRQueryClauseContext queryContext, 
			String clauseId, String parameterName, boolean left)
	{
		JRValueParameter valueParameter = queryContext.getValueParameter(parameterName);
		Object value = valueParameter.getValue();
		if (Date.class.isAssignableFrom(valueParameter.getValueClass()) || value instanceof Date)
		{
			// treat as regular parameter
			return new DefaultClauseFunctionParameterHandler(queryContext, parameterName, value);
		}

		if (value != null && !(value instanceof DateRange))
		{
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_UNSUPPORTED_PARAMETER_TYPE,
					new Object[]{parameterName, clauseId, value.getClass().getName()});
		}
		
		boolean useRangeStart = left ? isLeftClosed(clauseId) : !isRightClosed(clauseId);
		return new DateRangeParameterHandler(queryContext, parameterName, 
				(DateRange) value, useRangeStart);
	}

}
