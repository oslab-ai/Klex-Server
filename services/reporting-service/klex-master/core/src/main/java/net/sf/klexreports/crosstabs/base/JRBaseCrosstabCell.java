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

import java.io.Serializable;

import net.sf.klexreports.crosstabs.JRCellContents;
import net.sf.klexreports.crosstabs.JRCrosstabCell;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * Base read-only implementation of {@link net.sf.klexreports.crosstabs.JRCrosstabCell JRCrosstabCell}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseCrosstabCell implements JRCrosstabCell, Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	protected Integer width;
	protected Integer height;
	protected String rowTotalGroup;
	protected String columnTotalGroup;
	protected JRCellContents contents;
	
	protected JRBaseCrosstabCell()
	{
	}
	
	public JRBaseCrosstabCell(JRCrosstabCell crosstabCell, JRBaseObjectFactory factory)
	{
		factory.put(crosstabCell, this);
		
		width = crosstabCell.getWidth();
		height = crosstabCell.getHeight();
		
		rowTotalGroup = crosstabCell.getRowTotalGroup();
		columnTotalGroup = crosstabCell.getColumnTotalGroup();
		
		contents = factory.getCell(crosstabCell.getContents());
	}

	@Override
	public String getRowTotalGroup()
	{
		return rowTotalGroup;
	}

	@Override
	public String getColumnTotalGroup()
	{
		return columnTotalGroup;
	}

	@Override
	public JRCellContents getContents()
	{
		return contents;
	}

	@Override
	public Integer getHeight()
	{
		return height;
	}

	@Override
	public Integer getWidth()
	{
		return width;
	}

	@Override
	public Object clone() 
	{
		JRBaseCrosstabCell clone = null;

		try
		{
			clone = (JRBaseCrosstabCell)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
		
		clone.contents = JRCloneUtils.nullSafeClone(contents);

		return clone;
	}
}
