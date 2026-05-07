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
package net.sf.klexreports.engine.base;

import net.sf.klexreports.engine.ExpressionReturnValue;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * Base implementation of {@link net.sf.klexreports.engine.ExpressionReturnValue ExpressionReturnValue}.
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class BaseExpressionReturnValue extends BaseCommonReturnValue implements ExpressionReturnValue
{
	/**
	 * 
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */
	protected JRExpression expression;

	protected BaseExpressionReturnValue()
	{
	}

	
	protected BaseExpressionReturnValue(ExpressionReturnValue returnValue, JRBaseObjectFactory factory)
	{
		super(returnValue, factory);

		expression = factory.getExpression(returnValue.getExpression());
	}

	/**
	 * The expression producing the value to return.
	 */
	@Override
	public JRExpression getExpression()
	{
		return expression;
	}

	@Override
	public Object clone() 
	{
		BaseExpressionReturnValue clone = (BaseExpressionReturnValue)super.clone();
		clone.expression = JRCloneUtils.nullSafeClone(expression);
		return clone;
	}
}
