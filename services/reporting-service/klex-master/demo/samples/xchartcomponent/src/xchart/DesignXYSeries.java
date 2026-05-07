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
package xchart;

import java.io.Serializable;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.util.JRCloneUtils;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class DesignXYSeries implements XYSeries, Serializable
{


	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	protected JRExpression seriesExpression;
	protected JRExpression xValueExpression;
	protected JRExpression yValueExpression;
	protected JRExpression colorExpression;
	
	/**
	 *
	 */
	protected DesignXYSeries()
	{
	}
	
	
	/**
	 *
	 */
	public DesignXYSeries(XYSeries xySeries, JRBaseObjectFactory factory)
	{
		factory.put(xySeries, this);

		seriesExpression = factory.getExpression(xySeries.getSeriesExpression());
		xValueExpression = factory.getExpression(xySeries.getXValueExpression());
		yValueExpression = factory.getExpression(xySeries.getYValueExpression());
		colorExpression = factory.getExpression(xySeries.getColorExpression());
	}

	
	@Override
	public JRExpression getSeriesExpression()
	{
		return seriesExpression;
	}
	
	public void setSeriesExpression(JRExpression expression)
	{
		this.seriesExpression = expression;
	}
		
	@Override
	public JRExpression getXValueExpression()
	{
		return xValueExpression;
	}
		
	public void setXValueExpression(JRExpression expression)
	{
		this.xValueExpression = expression;
	}
		
	@Override
	public JRExpression getYValueExpression()
	{
		return yValueExpression;
	}
	
	public void setYValueExpression(JRExpression expression)
	{
		this.yValueExpression = expression;
	}
	
	@Override
	public JRExpression getColorExpression()
	{
		return colorExpression;
	}
		
	public void setColorExpression(JRExpression expression)
	{
		this.colorExpression = expression;
	}
		
	@Override
	public Object clone() 
	{
		DesignXYSeries clone = null;
		
		try
		{
			clone = (DesignXYSeries)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}

		clone.seriesExpression = JRCloneUtils.nullSafeClone(seriesExpression);
		clone.xValueExpression = JRCloneUtils.nullSafeClone(xValueExpression);
		clone.yValueExpression = JRCloneUtils.nullSafeClone(yValueExpression);
		clone.colorExpression = JRCloneUtils.nullSafeClone(colorExpression);
		
		return clone;
	}
}
