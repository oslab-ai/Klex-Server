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
package net.sf.klexreports.customizers.marker;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart.plot.IntervalMarker;

import net.sf.klexreports.engine.util.JRColorUtil;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class AbstractIntervalMarkerCustomizer extends AbstractMarkerCustomizer 
{
	public static final String PROPERTY_START_VALUE = "startValue";
	public static final String PROPERTY_END_VALUE = "endValue";
	public static final String PROPERTY_OUTLINE_COLOR = "outlineColor";


	protected IntervalMarker createMarker()
	{
		Double startValue = getDoubleProperty(PROPERTY_START_VALUE);
		Double endValue = getDoubleProperty(PROPERTY_END_VALUE);

		if (
			startValue == null 
			|| endValue == null
			)
		{
			return null;
		}

		IntervalMarker marker = new IntervalMarker(startValue, endValue);

		configureMarker(marker);
		
		Float strokeWidth = getFloatProperty(PROPERTY_STROKE_WIDTH);
		if (
			strokeWidth != null
			&& strokeWidth > 0
			) 
		{
			BasicStroke basicStroke = getStroke(strokeWidth);

			marker.setOutlineStroke(basicStroke);

			Color outlineColor = JRColorUtil.getColor(getProperty(PROPERTY_OUTLINE_COLOR), null);
			if (outlineColor != null)
			{
				marker.setOutlinePaint(outlineColor);
			}
		}

		return marker;
	}
}
