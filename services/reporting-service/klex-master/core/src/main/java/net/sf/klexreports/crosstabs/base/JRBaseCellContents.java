/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.crosstabs.base;

import java.awt.Color;

import net.sf.klexreports.crosstabs.JRCellContents;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRLineBox;
import net.sf.klexreports.engine.JRPropertiesHolder;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRStyle;
import net.sf.klexreports.engine.base.JRBaseElementGroup;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.type.ModeEnum;


/**
 * Base read-only implementation of {@link net.sf.klexreports.crosstabs.JRCellContents JRCellContents}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseCellContents extends JRBaseElementGroup implements JRCellContents
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	protected JRDefaultStyleProvider defaultStyleProvider;
	protected JRStyle style;
	protected String styleNameReference;
	
	protected ModeEnum mode;
	protected Color backcolor;
	protected JRLineBox lineBox;
	protected int width;
	protected int height;
	
	private JRPropertiesMap propertiesMap;

	public JRBaseCellContents(JRCellContents cell, JRBaseObjectFactory factory)
	{
		super(cell, factory);
		
		this.defaultStyleProvider = factory.getDefaultStyleProvider();
		style = factory.getStyle(cell.getStyle());
		styleNameReference = cell.getStyleNameReference();
		mode = cell.getMode();
		backcolor = cell.getBackcolor();
		lineBox = cell.getLineBox().clone(this);
		width = cell.getWidth();
		height = cell.getHeight();
		this.propertiesMap = JRPropertiesMap.getPropertiesClone(cell);
	}

	@Override
	public Color getBackcolor()
	{
		return backcolor;
	}

	@Override
	public JRLineBox getLineBox()
	{
		return lineBox;
	}

	@Override
	public int getWidth()
	{
		return width;
	}

	@Override
	public int getHeight()
	{
		return height;
	}

	@Override
	public JRDefaultStyleProvider getDefaultStyleProvider()
	{
		return defaultStyleProvider;
	}

	@Override
	public JRStyle getStyle()
	{
		return style;
	}

	@Override
	public ModeEnum getMode()
	{
		return mode;
	}

	@Override
	public String getStyleNameReference()
	{
		return styleNameReference;
	}

	@Override
	public Color getDefaultLineColor() 
	{
		return Color.black;
	}
	
	@Override
	public Object clone() 
	{
		JRBaseCellContents clone = (JRBaseCellContents) super.clone();
		clone.lineBox = lineBox == null ? null : (JRLineBox) lineBox.clone(clone);
		clone.propertiesMap = JRPropertiesMap.getPropertiesClone(this);
		return clone;
	}

	@Override
	public boolean hasProperties()
	{
		return propertiesMap != null && propertiesMap.hasProperties();
	}

	@Override
	public JRPropertiesMap getPropertiesMap()
	{
		if (propertiesMap == null)
		{
			propertiesMap = new JRPropertiesMap();
		}
		return propertiesMap;
	}

	@Override
	public JRPropertiesHolder getParentProperties()
	{
		return null;
	}
}
