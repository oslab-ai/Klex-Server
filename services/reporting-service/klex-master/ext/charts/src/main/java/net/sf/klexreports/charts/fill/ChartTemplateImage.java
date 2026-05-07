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
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JROrigin;
import net.sf.klexreports.engine.base.JRBasePen;
import net.sf.klexreports.engine.fill.JRTemplateImage;
import net.sf.klexreports.engine.fill.JRTemplatePrintImage;
import net.sf.klexreports.engine.type.FillEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 * @see JRTemplatePrintImage
 */
public class ChartTemplateImage extends JRTemplateImage
{
	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	

	/**
	 *
	 */
	protected ChartTemplateImage(JROrigin origin, JRDefaultStyleProvider defaultStyleProvider, JRChart chart)
	{
		super(origin, defaultStyleProvider);
		
		setChart(chart);
	}

	/**
	 *
	 */
	protected void setChart(JRChart chart)
	{
		super.setElement(chart);
		
		linePen = new JRBasePen(this);
		
		getLinePen().setLineWidth(0f);
		setFill(FillEnum.SOLID);
		
		copyLineBox(chart.getLineBox());

		setLinkType(chart.getLinkType());
		setLinkTarget(chart.getLinkTarget());
	}
}
