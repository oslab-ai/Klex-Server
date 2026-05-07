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
package net.sf.klexreports.components.charts;

import java.awt.Color;
import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.klexreports.charts.type.EdgeEnum;
import net.sf.klexreports.components.spiderchart.StandardChartSettings;
import net.sf.klexreports.engine.JRAnchor;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRFont;
import net.sf.klexreports.engine.JRHyperlink;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
@JsonDeserialize(as = StandardChartSettings.class)
public interface ChartSettings extends JRAnchor, JRHyperlink, Serializable
{

	/**
	 * 
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getShowLegend();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Color getBackcolor();

	/**
	 *
	 */
	public JRFont getTitleFont();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public EdgeEnum getTitlePosition();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Color getTitleColor();

	/**
	 *
	 */
	public JRFont getSubtitleFont();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Color getSubtitleColor();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Color getLegendBackgroundColor(); 

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public Color getLegendColor();
	
	/**
	 *
	 */
	public JRFont getLegendFont();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public EdgeEnum getLegendPosition();


	/**
	 *
	 */
	public JRExpression getTitleExpression();
 
	/**
	 *
	 */
	public JRExpression getSubtitleExpression();

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	public String getRenderType();
	
	/**
	 * Gets a user specified chart customizer class name.
	 * @see ChartCustomizer
 	 */
	@JacksonXmlProperty(isAttribute = true)
	public String getCustomizerClass();

}
