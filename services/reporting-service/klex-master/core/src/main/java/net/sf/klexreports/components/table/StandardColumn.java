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
package net.sf.klexreports.components.table;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class StandardColumn extends StandardBaseColumn implements Column
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public static final String PROPERTY_DETAIL = "detail";

	private Cell detail;
	
	public StandardColumn()
	{
	}
	
	public StandardColumn(Column column, ColumnFactory factory)
	{
		super(column, factory);
		
		Cell columnCell = column.getDetailCell();
		if (columnCell != null)
		{
			this.detail = new CompiledCell(columnCell, factory.getBaseObjectFactory());
		}
	}

	@Override
	public Cell getDetailCell()
	{
		return detail;
	}

	public void setDetailCell(Cell detail)
	{
		Object old = this.detail;
		this.detail = detail;
		getEventSupport().firePropertyChange(PROPERTY_DETAIL, old, this.detail);
	}

	@Override
	public <R> R visitColumn(ColumnVisitor<R> visitor)
	{
		return visitor.visitColumn(this);
	}

	@Override
	public Object clone()
	{
		StandardColumn clone = (StandardColumn) super.clone();
		clone.detail = JRCloneUtils.nullSafeClone(detail);
		return clone;
	}
}
