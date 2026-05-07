/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2022 TIBCO Software Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.components.subreport.fill;

import net.sf.klexreports.engine.JRDatasetParameter;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.JRSubreportParameter;

/**
 * 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SubreportParameterAdapter implements JRSubreportParameter
{

	private final JRDatasetParameter datasetParameter;
	
	public SubreportParameterAdapter(JRDatasetParameter datasetParameter)
	{
		this.datasetParameter = datasetParameter;
	}

	@Override
	public JRExpression getExpression()
	{
		return datasetParameter.getExpression();
	}

	@Override
	public String getName()
	{
		return datasetParameter.getName();
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
