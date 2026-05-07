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

import net.sf.klexreports.charts.JRChart;
import net.sf.klexreports.charts.JRChartPlot;
import net.sf.klexreports.charts.base.JRBasePie3DPlot;
import net.sf.klexreports.engine.JRConstants;



/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 * @deprecated To be removed.
 */
public class JRDesignPie3DPlot extends JRBasePie3DPlot
{


	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;


	@JsonCreator
	private JRDesignPie3DPlot()
	{
		this(null, null);
	}

	
	/**
	 *
	 */
	public JRDesignPie3DPlot(JRChartPlot pie3DPlot, JRChart chart)
	{
		super(pie3DPlot, chart, ChartCopyDesignObjectFactory.instance());
	}
}
