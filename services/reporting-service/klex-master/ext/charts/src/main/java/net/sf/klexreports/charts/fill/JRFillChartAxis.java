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

import net.sf.klexreports.charts.JRChart;
import net.sf.klexreports.charts.JRChartAxis;
import net.sf.klexreports.charts.JRMultiAxisPlot;
import net.sf.klexreports.charts.type.AxisPositionEnum;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;

/**
 * Describes an axis that can be added to a multiple axis chart.  The name
 * "axis" is a bit of a misnomer, as it really contains information about
 * a new dataset to plot, the axis to plot it against, and how to render that
 * dataset.
 *
 * @author Barry Klawans (barry@users.sourceforge.net)
 */
public class JRFillChartAxis implements JRChartAxis
{

	protected JRChartAxis parent;

	protected JRFillMultiAxisPlot fillMultiAxisPlot;
	
	/**
	 * The filled version of the <code>chart</code> field.  Contains evaluated
	 * expressions and data.
	 */
	protected JRFillChart fillChart;


	public JRFillChartAxis(JRChartAxis axis, ChartsFillObjectFactory factory)
	{
		JRFillObjectFactory parentFactory = factory.getParent();
		
		parentFactory.put(axis, this);

		this.parent = axis;
		this.fillMultiAxisPlot = (JRFillMultiAxisPlot)factory.getMultiAxisPlot(axis.getMultiAxisPlot());
		JRFillChart fillChart = (JRFillChart)parentFactory.getVisitResult(axis.getChart());
		this.fillChart = new JRFillChartForAxis(fillChart, factory, axis.getMultiAxisPlot().getChart());
	}

	/**
	 * Return the filled version of the chart with the dataset and plot for
	 * this axis.
	 *
	 * @return the filled version of the chart with the dataset and plot for
	 * 		   this axis
	 */
	public JRFillChart getFillChart()
	{
		return fillChart;
	}

	@Override
	public JRMultiAxisPlot getMultiAxisPlot()
	{
		return fillMultiAxisPlot;
	}

	@Override
	public JRChart getChart()
	{
		return parent.getChart();
	}

	@Override
	public AxisPositionEnum getPosition()
	{
		return parent.getPosition();
	}

	@Override
	public Object clone() 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JRChartAxis clone(JRMultiAxisPlot multiAxisPlot)
	{
		throw new UnsupportedOperationException();
	}
}
