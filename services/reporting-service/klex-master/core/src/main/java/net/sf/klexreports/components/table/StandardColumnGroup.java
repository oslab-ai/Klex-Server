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

import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class StandardColumnGroup extends StandardBaseColumn implements
		ColumnGroup
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public static final String PROPERTY_COLUMNS = "columns";

	private List<BaseColumn> children;
	
	public StandardColumnGroup()
	{
		children = new ArrayList<>();
	}
	
	public StandardColumnGroup(ColumnGroup columnGroup, ColumnFactory factory)
	{
		super(columnGroup, factory);
		
		children = factory.createColumns(columnGroup.getColumns());
	}
	
	@Override
	public List<BaseColumn> getColumns()
	{
		return children;
	}

	public void setColumns(List<BaseColumn> columns)
	{
		Object old = this.children;
		this.children = columns;
		getEventSupport().firePropertyChange(PROPERTY_COLUMNS, 
				old, this.children);
	}

	public void addColumn(BaseColumn column)
	{
		children.add(column);
		getEventSupport().fireCollectionElementAddedEvent(PROPERTY_COLUMNS, 
				column, children.size() - 1);
	}
	
	public void addColumn(int index, BaseColumn column)
	{
		children.add(index, column);
		getEventSupport().fireCollectionElementAddedEvent(PROPERTY_COLUMNS, 
				column, index);
	}

	public boolean removeColumn(BaseColumn column)
	{
		int idx = children.indexOf(column);
		if (idx >= 0)
		{
			children.remove(idx);
			getEventSupport().fireCollectionElementRemovedEvent(PROPERTY_COLUMNS, 
					column, idx);
		}
		return idx >= 0;
	}

	@Override
	public <R> R visitColumn(ColumnVisitor<R> visitor)
	{
		return visitor.visitColumnGroup(this);
	}

	@Override
	public Object clone()
	{
		StandardColumnGroup clone = (StandardColumnGroup) super.clone();
		clone.children = JRCloneUtils.cloneList(children);
		return clone;
	}

}
