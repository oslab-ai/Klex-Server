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
import java.util.Collection;
import java.util.SortedSet;

import net.sf.klexreports.charts.ChartsExpressionCollector;
import net.sf.klexreports.charts.JRCategoryAxisFormat;
import net.sf.klexreports.charts.JRChart;
import net.sf.klexreports.charts.JRChartPlot;
import net.sf.klexreports.charts.type.PlotOrientationEnum;
import net.sf.klexreports.charts.util.ChartsStyleResolver;
import net.sf.klexreports.engine.fill.JRFillBand;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRFillChartPlot implements JRChartPlot
{


	/**
	 *
	 */
	protected JRChartPlot parent;

	/**
	 *
	 */
	protected JRChart chart;


	/**
	 *
	 */
	protected JRFillChartPlot(JRChartPlot plot, ChartsFillObjectFactory factory)
	{
		JRFillObjectFactory parentFactory = factory.getParent();

		parentFactory.put(plot, this);

		parent = plot;
		
		chart = (JRChart)parentFactory.getVisitResult(plot.getChart());
	}

	protected void setBand(JRFillBand band)
	{
		//NOP
	}


	@Override
	public JRChart getChart()
	{
		return chart;
	}

	/**
	 *
	 */
	protected ChartsStyleResolver getStyleResolver() 
	{
		return new ChartsStyleResolver(getChart().getDefaultStyleProvider().getStyleResolver());
	}

	@Override
	public Color getBackcolor()
	{
		return getStyleResolver().getBackcolor(this);
	}
	
	@Override
	public Color getOwnBackcolor()
	{
		return parent.getOwnBackcolor();
	}
	
	@Override
	public void setBackcolor(Color backcolor)
	{
	}

	@Override
	public PlotOrientationEnum getOrientation()
	{
		return PlotOrientationEnum.getValueOrDefault(parent.getOrientation());
	}
		
	@Override
	public void setOrientation(PlotOrientationEnum orientation)
	{
		throw new UnsupportedOperationException();
	}
		
	@Override
	public Float getBackgroundAlpha()
	{
		return parent.getBackgroundAlpha();
	}
	
	@Override
	public void setBackgroundAlpha(Float BackgroundAlpha)
	{
	}
	
	@Override
	public Float getForegroundAlpha()
	{
		return parent.getForegroundAlpha();
	}
	
	@Override
	public void setForegroundAlpha(Float foregroundAlpha)
	{
	}
	
	/**
	 * @deprecated Replaced by {@link JRCategoryAxisFormat#getCategoryAxisTickLabelRotation()}.
	 */
	@Override
	public Double getLabelRotation()
	{
		return parent.getLabelRotation();
	}
	
	/**
	 * @deprecated Replaced by {@link JRCategoryAxisFormat#setCategoryAxisTickLabelRotation(Double)}.
	 */
	@Override
	public void setLabelRotation(Double labelRotation)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns a list of all the defined series colors.  Every entry in the list is of type JRChartPlot.JRSeriesColor.
	 * If there are no defined series colors this method will return an empty list, not null. 
	 */
	@Override
	public SortedSet<JRSeriesColor> getSeriesColors()
	{
		return parent.getSeriesColors();
	}
	
	/**
	 * Removes all defined series colors.
	 */
	@Override
	public void clearSeriesColors()
	{
	}
	
	/**
	 * Adds the specified series color to the plot.
	 */
	@Override
	public void addSeriesColor(JRSeriesColor seriesColor)
	{
	}

	@Override
	public void setSeriesColors(Collection<JRSeriesColor> colors)
	{
		// NOOP
	}
	
	@Override
	public void collectExpressions(ChartsExpressionCollector collector)
	{
	}

	@Override
	public Object clone() 
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Object clone(JRChart parentChart) 
	{
		throw new UnsupportedOperationException();
	}
}
