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

import net.sf.klexreports.crosstabs.JRCellContents;
import net.sf.klexreports.crosstabs.JRCrosstabColumnGroup;
import net.sf.klexreports.crosstabs.type.CrosstabColumnPositionEnum;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.util.JRCloneUtils;


/**
 * Base read-only implementation of crosstab column groups.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseCrosstabColumnGroup extends JRBaseCrosstabGroup implements JRCrosstabColumnGroup
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	protected int height;
	protected CrosstabColumnPositionEnum position = CrosstabColumnPositionEnum.LEFT;
	protected JRCellContents crosstabHeader;

	public JRBaseCrosstabColumnGroup(JRCrosstabColumnGroup group, JRBaseObjectFactory factory)
	{
		super(group, factory);
		
		height = group.getHeight();
		position = group.getPosition();
		crosstabHeader = factory.getCell(group.getCrosstabHeader());
	}

	@Override
	public CrosstabColumnPositionEnum getPosition()
	{
		return position;
	}

	@Override
	public int getHeight()
	{
		return height;
	}

	@Override
	public JRCellContents getCrosstabHeader()
	{
		return crosstabHeader;
	}

	@Override
	public Object clone()
	{
		JRBaseCrosstabColumnGroup clone = (JRBaseCrosstabColumnGroup) super.clone();
		clone.crosstabHeader = JRCloneUtils.nullSafeClone(crosstabHeader);
		return clone;
	}
}
