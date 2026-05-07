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
package net.sf.klexreports.engine.export.tabulator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class Row extends DimensionEntry
{
	private List<Cell> cells;
	
	public Row(int y)
	{
		super(y);
	}

	@Override
	public String toString()
	{
		return "row at " + super.toString();
	}
	
	public void setCell(Column column, Cell cell)
	{
		if (cells == null)
		{
			cells = new ArrayList<>();
		}
		
		int colIdx = column.index;
		if (colIdx >= cells.size())
		{
			for(int i = cells.size(); i <= colIdx; ++i)
			{
				cells.add(null);
			}
		}
		cells.set(colIdx, cell);
	}
	
	public Cell getCell(Column column)
	{
		if (cells == null || cells.size() <= column.index)
		{
			return null;
		}
		
		return cells.get(column.index);
	}
}