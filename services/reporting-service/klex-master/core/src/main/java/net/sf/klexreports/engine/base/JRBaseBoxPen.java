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
package net.sf.klexreports.engine.base;

import java.awt.Color;

import com.fasterxml.jackson.annotation.JsonCreator;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRLineBox;
import net.sf.klexreports.engine.JRPen;
import net.sf.klexreports.engine.type.LineStyleEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRBaseBoxPen extends JRBasePen implements JRBoxPen
{

	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	protected JRLineBox lineBox;
	
	/**
	 *
	 */
	public JRBaseBoxPen(JRLineBox box)
	{
		super(box);

		this.lineBox = box;
	}
	
	
	@JsonCreator
	private JRBaseBoxPen()
	{
		this(null);
	}
	
	@Override
	public JRLineBox getBox() 
	{
		return lineBox;
	}

	@Override
	public Float getLineWidth()
	{
		if (lineWidth != null)
		{
			return lineWidth;
		}
		Float penLineWidth = lineBox.getPen().getOwnLineWidth();
		if (penLineWidth != null) 
		{
			return penLineWidth;
		}
		return getStyleResolver().getParentLineWidth(this, penContainer.getDefaultLineWidth());
	}

	@Override
	public LineStyleEnum getLineStyle()
	{
		if (lineStyle != null)
		{
			return lineStyle;
		}
		LineStyleEnum penLineStyle = lineBox.getPen().getOwnLineStyle();
		if (penLineStyle != null)
		{
			return penLineStyle;
		}
		return getStyleResolver().getParentLineStyle(this);
	}

	@Override
	public Color getLineColor()
	{
		if (lineColor != null)
		{
			return lineColor;
		}
		Color penLineColor = lineBox.getPen().getOwnLineColor();
		if (penLineColor != null)
		{
			return penLineColor;
		}
		return getStyleResolver().getParentLineColor(this, penContainer.getDefaultLineColor());
	}

	@Override
	public JRPen getPen(JRLineBox box) 
	{
		return box.getPen();
	}

	@Override
	public JRBoxPen clone(JRLineBox lineBox)
	{
		JRBaseBoxPen clone = (JRBaseBoxPen)super.clone(lineBox);
		
		clone.lineBox = lineBox;
		
		return clone;
	}
	
}
