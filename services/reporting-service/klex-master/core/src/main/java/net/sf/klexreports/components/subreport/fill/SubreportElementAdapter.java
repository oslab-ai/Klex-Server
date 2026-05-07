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

import java.util.List;
import java.util.ListIterator;

import net.sf.klexreports.engine.ElementDecorator;
import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRDatasetParameter;
import net.sf.klexreports.engine.JRDatasetRun;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRSubreport;
import net.sf.klexreports.engine.JRSubreportParameter;
import net.sf.klexreports.engine.JRSubreportReturnValue;
import net.sf.klexreports.engine.ReturnValue;
import net.sf.klexreports.engine.type.OverflowType;

/**
 * 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SubreportElementAdapter extends ElementDecorator implements JRSubreport
{
	
	private final JRDatasetRun datasetRun;
	private final JRSubreportParameter[] parameters;
	private final JRSubreportReturnValue[] returnValues;

	public SubreportElementAdapter(JRDatasetRun datasetRun, JRComponentElement componentElement)
	{
		super(componentElement);
		
		this.datasetRun = datasetRun;
		if (datasetRun == null)
		{
			this.parameters = null;
			this.returnValues = null;
		}
		else
		{
			JRDatasetParameter[] datasetParameters = datasetRun.getParameters();
			if (datasetParameters == null)
			{
				this.parameters = null;
			}
			else
			{
				this.parameters = new JRSubreportParameter[datasetParameters.length];
				for (int i = 0; i < datasetParameters.length; i++)
				{
					JRDatasetParameter datasetParameter = datasetParameters[i];
					SubreportParameterAdapter subreportParameter = 
						new SubreportParameterAdapter(datasetParameter);
					this.parameters[i] = subreportParameter;
				}
			}
			
			List<ReturnValue> datasetReturnValues = datasetRun.getReturnValues();
			if (datasetReturnValues == null || datasetReturnValues.isEmpty())
			{
				returnValues = null;
			}
			else
			{
				returnValues = new JRSubreportReturnValue[datasetReturnValues.size()];
				for (ListIterator<ReturnValue> it = datasetReturnValues.listIterator(); it.hasNext();)
				{
					ReturnValue returnValue = it.next();
					returnValues[it.previousIndex()] = new SubreportReturnValueAdapter(returnValue);
				}
			}
		}
	}

	@Override
	public JRExpression getConnectionExpression()
	{
		return datasetRun == null ? null : datasetRun.getConnectionExpression();
	}

	@Override
	public JRExpression getDataSourceExpression()
	{
		return datasetRun == null ? null : datasetRun.getDataSourceExpression();
	}

	@Override
	public JRExpression getExpression()
	{
		// no report expression
		return null;
	}

	@Override
	public JRSubreportParameter[] getParameters()
	{
		return parameters;
	}

	@Override
	public JRExpression getParametersMapExpression()
	{
		return datasetRun == null ? null : datasetRun.getParametersMapExpression();
	}

	@Override
	public JRSubreportReturnValue[] getReturnValues()
	{
		return returnValues;
	}

	@Override
	public Boolean getUsingCache()
	{
		return Boolean.FALSE;
	}

	@Override
	public Boolean isRunToBottom()
	{
		return false;
	}

	@Override
	public void setRunToBottom(Boolean runToBottom)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setUsingCache(Boolean isUsingCache)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public OverflowType getOverflowType()
	{
		// default
		return null;
	}

	@Override
	public void setOverflowType(OverflowType overflowType)
	{
		throw new UnsupportedOperationException();
	}

}
