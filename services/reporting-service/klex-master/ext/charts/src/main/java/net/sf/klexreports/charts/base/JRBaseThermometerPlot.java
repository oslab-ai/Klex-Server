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

import java.awt.Color;

import net.sf.klexreports.charts.ChartsExpressionCollector;
import net.sf.klexreports.charts.JRChart;
import net.sf.klexreports.charts.JRChartPlot;
import net.sf.klexreports.charts.JRDataRange;
import net.sf.klexreports.charts.JRThermometerPlot;
import net.sf.klexreports.charts.JRValueDisplay;
import net.sf.klexreports.charts.type.ValueLocationEnum;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * An immutable representation of the layout of a thermometer plot.
 *
 * @author Barry Klawans (bklawans@users.sourceforge.net)
 */
public class JRBaseThermometerPlot extends JRBaseChartPlot implements JRThermometerPlot
{
	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 * The range of values that can be displayed by this thermometer.  Specifies
	 * the upper and lower bounds of the meter itself.
	 */
	protected JRDataRange dataRange;

	/**
	 * Formatting information for the textual display of the value, including
	 * font, color and a mask.
	 */
	protected JRValueDisplay valueDisplay;

	/**
	 * Specifies where the textual display of the value should be shown.
	 */
	protected ValueLocationEnum valueLocationObject;

	/**
	 * The default color to use for the mercury in the thermometer.
	 */
	protected Color mercuryColor;

	/**
	 * The boundaries of the low range.
	 */
	protected JRDataRange lowRange;

	/**
	 * The boundaries of the medium range.
	 */
	protected JRDataRange mediumRange;

	/**
	 * The boundaries of the high range.
	 */
	protected JRDataRange highRange;

	/**
	 * Constructs a new thermometer plot that is a copy of an existing one.
	 *
	 * @param plot the plot to copy
	 * @param chart the parent chart
	 */
	public JRBaseThermometerPlot(JRChartPlot plot, JRChart chart)
	{
		super(plot, chart);
		
		JRThermometerPlot thermoPlot = plot instanceof JRThermometerPlot ? (JRThermometerPlot)plot : null;
		if (thermoPlot == null)
		{
			valueDisplay = new JRBaseValueDisplay(null, chart);
		}
		else
		{
			valueDisplay = new JRBaseValueDisplay(thermoPlot.getValueDisplay(), chart);
		}
	}

	/**
	 * Constructs a new plot that is a copy of an existing one, and registers
	 * all expression used by the plot with the specified factory.
	 *
	 * @param thermoPlot the plot to copy
	 * @param factory the factory to register any expressions with
	 */
	public JRBaseThermometerPlot(JRThermometerPlot thermoPlot, ChartsBaseObjectFactory factory)
	{
		super(thermoPlot, factory);

		dataRange = new JRBaseDataRange(thermoPlot.getDataRange(), factory);

		valueDisplay = new JRBaseValueDisplay(thermoPlot.getValueDisplay(), factory);

		valueLocationObject = thermoPlot.getValueLocation();

		mercuryColor = thermoPlot.getMercuryColor();

		if (thermoPlot.getLowRange() != null)
		{
			lowRange = new JRBaseDataRange(thermoPlot.getLowRange(), factory);
		}
		if (thermoPlot.getMediumRange() != null)
		{
			mediumRange = new JRBaseDataRange(thermoPlot.getMediumRange(), factory);
		}
		if (thermoPlot.getHighRange() != null)
		{
			highRange = new JRBaseDataRange(thermoPlot.getHighRange(), factory);
		}
	}

	@Override
	public JRDataRange getDataRange()
	{
		return dataRange;
	}

	@Override
	public JRValueDisplay getValueDisplay()
	{
		return valueDisplay;
	}

	@Override
	public ValueLocationEnum getValueLocation()
	{
		return valueLocationObject;
	}

	@Override
	public Color getMercuryColor()
	{
		return mercuryColor;
	}

	@Override
	public JRDataRange getLowRange()
	{
		return lowRange;
	}

	@Override
	public JRDataRange getMediumRange()
	{
		return mediumRange;
	}

	@Override
	public JRDataRange getHighRange()
	{
		return highRange;
	}

	/**
	 * Adds all the expression used by this plot with the specified collector.
	 * All collected expression that are also registered with a factory will
	 * be included with the report is compiled.
	 *
	 * @param collector the expression collector to use
	 */
	@Override
	public void collectExpressions(ChartsExpressionCollector collector)
	{
		collector.collect(this);
	}

	@Override
	public Object clone(JRChart parentChart) 
	{
		JRBaseThermometerPlot clone = (JRBaseThermometerPlot)super.clone(parentChart);
		clone.dataRange = JRCloneUtils.nullSafeClone(dataRange);
		clone.valueDisplay = valueDisplay == null ? null : valueDisplay.clone(parentChart);
		clone.lowRange = JRCloneUtils.nullSafeClone(lowRange);
		clone.mediumRange = JRCloneUtils.nullSafeClone(mediumRange);
		clone.highRange = JRCloneUtils.nullSafeClone(highRange);
		return clone;
	}
}
