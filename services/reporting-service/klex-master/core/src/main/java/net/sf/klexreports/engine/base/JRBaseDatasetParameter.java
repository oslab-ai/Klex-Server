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
import net.sf.klexreports.engine.JRDatasetParameter;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRRuntimeException;


/**
 * Base read-only implementation of {@link net.sf.klexreports.engine.JRDatasetParameter JRDatasetParameter}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseDatasetParameter implements JRDatasetParameter, Serializable
{


	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */
	protected String name;
	protected JRExpression expression;


	/**
	 *
	 */
	protected JRBaseDatasetParameter()
	{
	}
	
	
	/**
	 *
	 */
	protected JRBaseDatasetParameter(JRDatasetParameter datasetParameter, JRBaseObjectFactory factory)
	{
		factory.put(datasetParameter, this);

		name = datasetParameter.getName();
		expression = factory.getExpression(datasetParameter.getExpression());
	}
		

	@Override
	public String getName()
	{
		return this.name;
	}
		
	@Override
	public JRExpression getExpression()
	{
		return this.expression;
	}

	
	@Override
	public Object clone() 
	{
		try
		{
			return super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
	}

}
