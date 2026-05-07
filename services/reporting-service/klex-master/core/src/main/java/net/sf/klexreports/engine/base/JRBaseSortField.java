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

import java.io.Serializable;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.JRSortField;
import net.sf.klexreports.engine.design.events.JRChangeEventsSupport;
import net.sf.klexreports.engine.design.events.JRPropertyChangeSupport;
import net.sf.klexreports.engine.type.SortFieldTypeEnum;
import net.sf.klexreports.engine.type.SortOrderEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRBaseSortField implements JRSortField, Serializable, JRChangeEventsSupport
{
	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public static final String PROPERTY_ORDER = "order";

	/**
	 *
	 */
	protected String name;
	protected SortOrderEnum order = SortOrderEnum.ASCENDING;
	protected SortFieldTypeEnum type = SortFieldTypeEnum.FIELD;


	/**
	 *
	 */
	protected JRBaseSortField()
	{
	}
	
	
	/**
	 *
	 */
	protected JRBaseSortField(JRSortField sortField, JRBaseObjectFactory factory)
	{
		factory.put(sortField, this);
		
		name = sortField.getName();
		order = sortField.getOrder();
		type = sortField.getType();
	}
		

	@Override
	public String getName()
	{
		return name;
	}
		
	@Override
	public SortOrderEnum getOrder()
	{
		return order;
	}
		
	/**
	 *
	 */
	public void setOrder(SortOrderEnum order)
	{
		Object old = this.order;
		this.order = order;
		getEventSupport().firePropertyChange(PROPERTY_ORDER, old, this.order);
	}
	
	@Override
	public SortFieldTypeEnum getType()
	{
		return type;
	}
		
	@Override
	public Object clone() 
	{
		try
		{
			JRBaseSortField clone = (JRBaseSortField)super.clone(); 
			clone.eventSupport = null;
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
	}
	
	private transient JRPropertyChangeSupport eventSupport;
	
	@Override
	public JRPropertyChangeSupport getEventSupport()
	{
		synchronized (this)
		{
			if (eventSupport == null)
			{
				eventSupport = new JRPropertyChangeSupport(this);
			}
		}
		
		return eventSupport;
	}
}
