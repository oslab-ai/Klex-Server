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

import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRPropertiesHolder;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.type.ParameterEvaluationTimeEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRFillParameter implements JRValueParameter
{


	/**
	 *
	 */
	protected JRParameter parent;

	/**
	 *
	 */
	private Object value;


	/**
	 *
	 */
	protected JRFillParameter(
		JRParameter parameter, 
		JRFillObjectFactory factory
		)
	{
		factory.put(parameter, this);

		parent = parameter;
	}


	@Override
	public String getName()
	{
		return parent.getName();
	}
		
	@Override
	public String getDescription()
	{
		return parent.getDescription();
	}
		
	@Override
	public void setDescription(String description)
	{
	}
	
	@Override
	public Class<?> getValueClass()
	{
		return parent.getValueClass();
	}
	
	@Override
	public String getValueClassName()
	{
		return parent.getValueClassName();
	}

	@Override
	public Class<?> getNestedType()
	{
		return parent.getNestedType();
	}

	@Override
	public String getNestedTypeName()
	{
		return parent.getNestedTypeName();
	}
	
	@Override
	public boolean isSystemDefined()
	{
		return parent.isSystemDefined();
	}
	
	@Override
	public boolean isForPrompting()
	{
		return parent.isForPrompting();
	}
	
	@Override
	public ParameterEvaluationTimeEnum getEvaluationTime()
	{
		return parent.getEvaluationTime();
	}
	
	@Override
	public JRExpression getDefaultValueExpression()
	{
		return parent.getDefaultValueExpression();
	}
		
	@Override
	public Object getValue()
	{
		return value;
	}
		
	@Override
	public void setValue(Object value)
	{
		this.value = value;
	}

	
	@Override
	public boolean hasProperties()
	{
		return parent.hasProperties();
	}


	@Override
	public JRPropertiesMap getPropertiesMap()
	{
		return parent.getPropertiesMap();
	}

	
	@Override
	public JRPropertiesHolder getParentProperties()
	{
		return null;
	}
		

	@Override
	public Object clone() 
	{
		throw new UnsupportedOperationException();
	}

}
