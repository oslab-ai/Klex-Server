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

import net.sf.klexreports.crosstabs.CrosstabColumnCell;
import net.sf.klexreports.crosstabs.JRCellContents;
import net.sf.klexreports.crosstabs.type.CrosstabColumnPositionEnum;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BaseCrosstabColumnCell implements CrosstabColumnCell, Serializable
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	protected int height;
	protected CrosstabColumnPositionEnum contentsPosition;
	protected JRCellContents cellContents;

	// used by the design implementation
	protected BaseCrosstabColumnCell()
	{
	}

	public BaseCrosstabColumnCell(CrosstabColumnCell cell, JRBaseObjectFactory factory)
	{
		factory.put(cell, this);
		
		height = cell.getHeight();
		contentsPosition = cell.getContentsPosition();
		cellContents = factory.getCell(cell.getCellContents());
	}
	
	@Override
	public int getHeight()
	{
		return height;
	}

	@Override
	public CrosstabColumnPositionEnum getContentsPosition()
	{
		return contentsPosition;
	}

	@Override
	public JRCellContents getCellContents()
	{
		return cellContents;
	}
	
	@Override
	public Object clone()
	{
		try
		{
			BaseCrosstabColumnCell clone = (BaseCrosstabColumnCell) super.clone();
			clone.cellContents = JRCloneUtils.nullSafeClone(cellContents);
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			// never
			throw new JRRuntimeException(e);
		}
	}

}
