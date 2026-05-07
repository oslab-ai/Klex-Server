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
package net.sf.klexreports.engine.design;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

import net.sf.klexreports.engine.JRChild;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.JRElementGroup;
import net.sf.klexreports.engine.base.JRBaseElementGroup;
import net.sf.klexreports.engine.design.events.JRChangeEventsSupport;
import net.sf.klexreports.engine.design.events.JRPropertyChangeSupport;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRDesignElementGroup extends JRBaseElementGroup implements JRChangeEventsSupport
{


	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;


	public static final String PROPERTY_ELEMENT_GROUP = "elementGroup";

	public static final String PROPERTY_CHILDREN = "children";

	/**
	 *
	 */
	public void setElementGroup(JRElementGroup elementGroup)
	{
		Object old = this.elementGroup;
		this.elementGroup = elementGroup;
		getEventSupport().firePropertyChange(PROPERTY_ELEMENT_GROUP, old, this.elementGroup);
	}

	/**
	 *
	 */
	@JsonSetter
	private void setChildren(List<JRChild> children)
	{
		if (children != null)
		{
			for (JRChild child : children)
			{
				if (child instanceof JRDesignElement)
				{
					addElement((JRDesignElement)child);
				}
				else
				{
					addElementGroup((JRDesignElementGroup)child);
				}
			}
		}
	}

	/**
	 *
	 */
	public void addElement(JRDesignElement element)
	{
		addElement(children.size(), element);
	}

	/**
	 *
	 */
	public void addElement(int index, JRDesignElement element)
	{
		element.setElementGroup(this);
		
		this.children.add(index, element);
		getEventSupport().fireCollectionElementAddedEvent(PROPERTY_CHILDREN, element, index);
	}

	public void addElement(JRElement element)
	{
		this.children.add(element);
		getEventSupport().fireCollectionElementAddedEvent(PROPERTY_CHILDREN, element, this.children.size() - 1);
	}

	/**
	 *
	 */
	public JRDesignElement removeElement(JRDesignElement element)
	{
		element.setElementGroup(null);

		int idx = this.children.indexOf(element);
		if (idx >= 0)
		{
			this.children.remove(idx);
			getEventSupport().fireCollectionElementRemovedEvent(PROPERTY_CHILDREN, element, idx);
		}
		
		return element;
	}

	/**
	 *
	 */
	public void addElementGroup(JRDesignElementGroup elemGrp)
	{
		addElementGroup(children.size(), elemGrp);
	}

	/**
	 *
	 */
	public void addElementGroup(int index, JRDesignElementGroup elemGrp)
	{
		elemGrp.setElementGroup(this);
		
		this.children.add(index, elemGrp);
		getEventSupport().fireCollectionElementAddedEvent(PROPERTY_CHILDREN, elemGrp, index);
	}

	/**
	 *
	 */
	public JRDesignElementGroup removeElementGroup(JRDesignElementGroup elemGrp)
	{
		elemGrp.setElementGroup(null);

		int idx = this.children.indexOf(elemGrp);
		if (idx >= 0)
		{
			this.children.remove(idx);
			getEventSupport().fireCollectionElementRemovedEvent(PROPERTY_CHILDREN, elemGrp, idx);
		}
		
		return elemGrp;
	}

	@Override
	public Object clone()
	{
		JRDesignElementGroup clone = (JRDesignElementGroup)super.clone();
		clone.eventSupport = null;
		return clone;
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
