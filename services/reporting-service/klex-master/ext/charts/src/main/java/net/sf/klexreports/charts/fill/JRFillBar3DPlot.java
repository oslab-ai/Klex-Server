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

import net.sf.klexreports.charts.JRBar3DPlot;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 * @deprecated To be removed.
 */
public class JRFillBar3DPlot extends JRFillBarPlot implements JRBar3DPlot
{
	/**
	 * 
	 */
	public JRFillBar3DPlot( JRBar3DPlot barPlot, ChartsFillObjectFactory factory )
	{
		super( barPlot, factory );
	}

	@Override
	public Double getXOffset() 
	{
		return ((JRBar3DPlot)parent).getXOffset();
	}

	@Override
	public void setXOffset(Double xOffset) 
	{
	}

	@Override
	public Double getYOffset() 
	{
		return ((JRBar3DPlot)parent).getYOffset();
	}

	@Override
	public void setYOffset(Double yOffset) 
	{
	}
}
