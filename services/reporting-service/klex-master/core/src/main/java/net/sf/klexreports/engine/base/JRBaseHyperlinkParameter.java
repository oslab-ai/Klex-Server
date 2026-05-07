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

import java.io.Serializable;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRHyperlinkParameter;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.util.JRCloneUtils;


/**
 * Base implementation of {@link JRHyperlinkParameter JRHyperlinkParameter}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseHyperlinkParameter implements JRHyperlinkParameter, Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	protected String name;
	protected JRExpression valueExpression;
	
	
	/**
	 * Creates a blank instance.
	 */
	protected JRBaseHyperlinkParameter()
	{
	}

	
	/**
	 * Creates an instance equivalent to an existing hyperlink parameter.
	 * 
	 * @param parameter the parameter to replicate
	 * @param factory the base object factory
	 */
	public JRBaseHyperlinkParameter(JRHyperlinkParameter parameter, JRBaseObjectFactory factory)
	{
		factory.put(parameter, this);

		this.name = parameter.getName();
		this.valueExpression = factory.getExpression(parameter.getValueExpression());
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public JRExpression getValueExpression()
	{
		return valueExpression;
	}

	@Override
	public Object clone() 
	{
		JRBaseHyperlinkParameter clone = null;

		try
		{
			clone = (JRBaseHyperlinkParameter)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}

		clone.valueExpression = JRCloneUtils.nullSafeClone(valueExpression);
		
		return clone;
	}

	
}
