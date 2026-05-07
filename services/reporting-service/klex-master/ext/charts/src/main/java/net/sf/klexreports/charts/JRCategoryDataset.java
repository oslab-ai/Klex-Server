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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import net.sf.klexreports.charts.design.JRDesignCategoryDataset;

/**
 * This dataset accommodates one or more data series consisting of values associated with 
 * categories. It is used to render Bar, Stacked Bar, Line, Area, and Stacked Area 
 * charts 
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
@JsonTypeName("category")
@JsonDeserialize(as = JRDesignCategoryDataset.class)
public interface JRCategoryDataset extends JRChartDataset
{
	
	/**
	 * @return an array of {@link JRCategorySeries} objects representing the 
	 * series for category charts
	 * 
	 * @see JRCategorySeries
	 */
	@JacksonXmlElementWrapper(useWrapping = false)
	public JRCategorySeries[] getSeries();

}
