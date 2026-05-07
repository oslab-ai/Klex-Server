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
package net.sf.klexreports.charts.base;

import java.io.Serializable;

import net.sf.klexreports.charts.JRPieSeries;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRHyperlink;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.util.JRCloneUtils;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRBasePieSeries implements JRPieSeries, Serializable
{


	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	protected JRExpression keyExpression;
	protected JRExpression valueExpression;
	protected JRExpression labelExpression;
	protected JRHyperlink sectionHyperlink;

	
	/**
	 *
	 */
	protected JRBasePieSeries()
	{
	}
	
	
	/**
	 *
	 */
	public JRBasePieSeries(JRPieSeries pieSeries, ChartsBaseObjectFactory factory)
	{
		JRBaseObjectFactory parentFactory = factory.getParent();
		parentFactory.put(pieSeries, this);

		keyExpression = parentFactory.getExpression(pieSeries.getKeyExpression());
		valueExpression = parentFactory.getExpression(pieSeries.getValueExpression());
		labelExpression = parentFactory.getExpression(pieSeries.getLabelExpression());
		sectionHyperlink = parentFactory.getHyperlink(pieSeries.getSectionHyperlink());
	}

	
	@Override
	public JRExpression getKeyExpression()
	{
		return keyExpression;
	}
		
	@Override
	public JRExpression getValueExpression()
	{
		return valueExpression;
	}
		
	@Override
	public JRExpression getLabelExpression()
	{
		return labelExpression;
	}

	
	@Override
	public JRHyperlink getSectionHyperlink()
	{
		return sectionHyperlink;
	}
		
	@Override
	public Object clone() 
	{
		JRBasePieSeries clone = null;
		
		try
		{
			clone = (JRBasePieSeries)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
		
		clone.keyExpression = JRCloneUtils.nullSafeClone(keyExpression);
		clone.valueExpression = JRCloneUtils.nullSafeClone(valueExpression);
		clone.labelExpression = JRCloneUtils.nullSafeClone(labelExpression);
		clone.sectionHyperlink = JRCloneUtils.nullSafeClone(sectionHyperlink);
		
		return clone;
	}
}
