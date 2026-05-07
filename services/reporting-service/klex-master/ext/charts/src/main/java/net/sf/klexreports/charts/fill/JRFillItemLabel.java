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
package net.sf.klexreports.charts.fill;

import java.awt.Color;

import net.sf.klexreports.charts.JRChart;
import net.sf.klexreports.charts.JRItemLabel;
import net.sf.klexreports.engine.JRFont;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;

/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class JRFillItemLabel implements JRItemLabel
{

	/**
	 *
	 */
	protected JRItemLabel parent;

	/**
	 *
	 */
	protected JRChart chart;

	/**
	 *
	 */
	public JRFillItemLabel(JRItemLabel itemLabel, JRFillObjectFactory factory)
	{
		factory.put(itemLabel, this);

		parent = itemLabel;
		
		chart = (JRChart)factory.getVisitResult(itemLabel.getChart());
	}

	@Override
	public JRChart getChart()
	{
		return chart;
	}
	
	@Override
	public Color getColor()
	{
		return parent.getColor();
	}

	@Override
	public Color getBackgroundColor()
	{
		return parent.getBackgroundColor();
	}

	/**
	 *
	 */
//	public String getMask(){
//		return parent.getMask();
//	}

	@Override
	public JRFont getFont()
	{
		return parent.getFont();
	}
	
	@Override
	public Object clone() 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JRItemLabel clone(JRChart parentChart)
	{
		throw new UnsupportedOperationException();
	}
}
