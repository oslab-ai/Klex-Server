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
package net.sf.klexreports.components.spiderchart;

import java.awt.Color;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.klexreports.components.charts.ChartPlot;
import net.sf.klexreports.components.spiderchart.type.SpiderRotationEnum;
import net.sf.klexreports.components.spiderchart.type.TableOrderEnum;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRFont;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public interface SpiderPlot extends ChartPlot
{

	/**
	 * 
	 */
	public JRExpression getMaxValueExpression();
	

	/**
	 * 
	 */
	public JRFont getLabelFont();

	
	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public SpiderRotationEnum getRotation();

	
	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public TableOrderEnum getTableOrder();

	
	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getWebFilled();
	

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Double getStartAngle();
	

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Double getHeadPercent();
	

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Double getInteriorGap();
	

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Color getAxisLineColor();
	

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Float getAxisLineWidth();
	

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Double getLabelGap();
	

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Color getLabelColor();
}
