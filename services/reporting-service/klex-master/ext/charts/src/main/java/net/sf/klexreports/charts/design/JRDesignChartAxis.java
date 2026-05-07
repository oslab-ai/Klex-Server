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
package net.sf.klexreports.charts.design;

import com.fasterxml.jackson.annotation.JsonCreator;

import net.sf.klexreports.charts.JRChartAxis;
import net.sf.klexreports.charts.JRMultiAxisPlot;
import net.sf.klexreports.charts.base.JRBaseChartAxis;
import net.sf.klexreports.charts.type.AxisPositionEnum;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.design.events.JRChangeEventsSupport;
import net.sf.klexreports.engine.design.events.JRPropertyChangeSupport;

/**
 * {@link JRChartAxis JRChartAxis} implementation to be used for report design.
 *
 * @author Barry Klawans (barry@users.sourceforge.net)
 */
public class JRDesignChartAxis extends JRBaseChartAxis implements JRChangeEventsSupport
{
	
	public static final String PROPERTY_CHART = "chart";
	
	public static final String PROPERTY_POSITION = "position";

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	@JsonCreator
	private JRDesignChartAxis()
	{
		this(null);
	}

	/**
	 * Construct a new axis that will be added to the specified multiaxis chart plot.
	 *
	 * @param multiAxisPlot the plot that the axis will be added to
	 */
	public JRDesignChartAxis(JRMultiAxisPlot multiAxisPlot)
	{
		this.multiAxisPlot = multiAxisPlot;
	}

	/**
	 * Sets the position of this axis' value line relative to the multiple
	 * axis chart.
	 *
	 * @param position the position of this axis
	 */
	public void setPosition(AxisPositionEnum position)
	{
		AxisPositionEnum old = this.position;
		this.position = position;
		getEventSupport().firePropertyChange(PROPERTY_POSITION, old, this.position);
	}

	/**
	 * Set the chart that contains the dataset and plot to use for this
	 * axis.  The plot is used to figure out how to render the dataset (ie
	 * as a line or bar chart) when adding it to the multiple axis chart.
	 *
	 * @param chart the chart that contains the dataset and plot for this axis
	 */
	public void setChart(JRDesignChart chart)
	{
		Object old = this.chart;
		this.chart = chart;
		getEventSupport().firePropertyChange(PROPERTY_CHART, old, this.chart);
	}

	/**
	 * Sets the chart that contains the dataset and plot for this axis.
	 * Identical to {@link #setChart} but is called by the XML digester
	 * when parsing the report source.
	 *
	 * @param element
	 */
	public void addElement(JRElement element)
	{
		setChart((JRDesignChart)element);
	}
	
	@Override
	public Object clone() 
	{
		JRDesignChartAxis clone = (JRDesignChartAxis)super.clone();
		clone.eventSupport = null;
		return clone;
	}
	
	private transient JRPropertyChangeSupport eventSupport;
	
	@Override
	public JRPropertyChangeSupport getEventSupport()
	{
		synchronized (this)
		{
			if (eventSupport == null)
			{
				eventSupport = new JRPropertyChangeSupport(this);
			}
		}
		
		return eventSupport;
	}
}
