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

import java.util.Locale;
import java.util.TimeZone;

import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.style.StyleProviderContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class FillStyleProviderContext implements StyleProviderContext
{
	private JRFillElement element;

	
	/**
	 *
	 */
	protected FillStyleProviderContext(JRFillElement element)
	{
		this.element = element;
	}

	
	@Override
	public KlexReportsContext getKlexReportsContext()
	{
		return element.filler.getKlexReportsContext();
	}


	@Override
	public JRElement getElement()
	{
		return element;
	}


	@Override
	public Object evaluateExpression(JRExpression expression, byte evaluation)
	{
		try
		{
			return element.evaluateExpression(expression, evaluation);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}


	@Override
	public Object getFieldValue(String fieldName, byte evaluation)
	{
		return element.getField(fieldName).getValue(evaluation);
	}


	@Override
	public Object getVariableValue(String variableName, byte evaluation)
	{
		return element.getVariable(variableName).getValue(evaluation);
	}


	@Override
	public Locale getLocale()
	{
		return element.filler.getLocale();
	}


	@Override
	public TimeZone getTimeZone()
	{
		return element.getTimeZone();
	}
}
