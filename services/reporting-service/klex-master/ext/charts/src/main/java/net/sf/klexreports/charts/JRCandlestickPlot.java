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
package net.sf.klexreports.charts;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.klexreports.charts.design.JRDesignCandlestickPlot;
import net.sf.klexreports.engine.JRExpression;


/**
 * The Candlestick plot is also an axis-oriented plot and allows you to customize axis labels 
 * using expressions. It can be used only in combination with a Candlestick chart.
 * <br/>
 * The Candlestick chart uses a High-Low dataset, but unlike the High-Low chart, the 
 * Candlestick chart can make use of the volume value inside each dataset item.
 * <br/>
 * The volume value is displayed as the body of the candlestick figure rendered for each 
 * item. The volume is displayed by default in a Candlestick chart but can be suppressed by 
 * setting the <code>isShowVolume</code> flag to false.
 * 
 * @author Ionut Nedelcu (ionutned@users.sourceforge.net)
 */
@JsonTypeName("candlestick")
@JsonDeserialize(as = JRDesignCandlestickPlot.class)
public interface JRCandlestickPlot extends JRChartPlot, JRTimeAxisFormat, JRValueAxisFormat
{

	/**
	 * 
	 */
	public JRExpression getTimeAxisLabelExpression();
	
	/**
	 * 
	 */
	public JRExpression getValueAxisLabelExpression();

	/**
	 * 
	 */
	public JRExpression getDomainAxisMinValueExpression();

	/**
	 * 
	 */
	public JRExpression getDomainAxisMaxValueExpression();

	/**
	 * 
	 */
	public JRExpression getRangeAxisMinValueExpression();

	/**
	 * 
	 */
	public JRExpression getRangeAxisMaxValueExpression();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getShowVolume();

}
