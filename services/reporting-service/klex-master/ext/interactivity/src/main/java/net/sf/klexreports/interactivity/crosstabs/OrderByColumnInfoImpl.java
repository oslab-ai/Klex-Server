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
package net.sf.klexreports.interactivity.crosstabs;

import java.util.List;

import net.sf.klexreports.crosstabs.fill.calculation.ColumnValueInfo;
import net.sf.klexreports.crosstabs.fill.calculation.OrderByColumnInfo;
import net.sf.klexreports.engine.type.SortOrderEnum;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class OrderByColumnInfoImpl implements OrderByColumnInfo
{

	private SortOrderEnum order;
	private int measureIndex;
	private List<ColumnValueInfo> columnValues;

	@Override
	public SortOrderEnum getOrder()
	{
		return order;
	}

	public void setOrder(SortOrderEnum order)
	{
		this.order = order;
	}

	@Override
	public List<ColumnValueInfo> getColumnValues()
	{
		return columnValues;
	}

	public void setColumnValues(List<ColumnValueInfo> columnValues)
	{
		this.columnValues = columnValues;
	}

	@Override
	public int getMeasureIndex()
	{
		return measureIndex;
	}

	public void setMeasureIndex(int measureIndex)
	{
		this.measureIndex = measureIndex;
	}
	
}
