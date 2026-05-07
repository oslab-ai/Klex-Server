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

import net.sf.klexreports.engine.JRCloneable;
import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.JRVisitable;
import net.sf.klexreports.engine.JRVisitor;
import net.sf.klexreports.engine.component.BaseComponentContext;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ContextAwareComponent;
import net.sf.klexreports.engine.design.events.JRPropertyChangeSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link JRComponentElement} implementation which is to be used at report
 * design time.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRDesignComponentElement extends JRDesignElement implements JRComponentElement
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private static final Log log = LogFactory.getLog(JRDesignComponentElement.class);
	
	public static final String PROPERTY_COMPONENT = "component";
	
	private Component component;
	
	private transient JRPropertyChangeSupport eventSupport;

	/**
	 * Creates an empty component element.
	 */
	public JRDesignComponentElement()
	{
		super(null);
	}

	/**
	 * Creates an empty component element.
	 * 
	 * @param defaultStyleProvider the default style provide to use for this
	 * element
	 */
	public JRDesignComponentElement(JRDefaultStyleProvider defaultStyleProvider)
	{
		super(defaultStyleProvider);
	}
	
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
	
	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		collector.collect(this);
	}

	@Override
	public void visit(JRVisitor visitor)
	{
		visitor.visitComponentElement(this);
		
		if (component instanceof JRVisitable)
		{
			((JRVisitable) component).visit(visitor);
		}
	}

	@Override
	public Component getComponent()
	{
		return component;
	}

	/**
	 * Sets the component instance wrapped by this element.
	 * 
	 * @param component the component instance
	 * @see #getComponent()
	 */
	public void setComponent(Component component)
	{
		Object old = this.component;
		this.component = component;
		setComponentContext();
		getEventSupport().firePropertyChange(PROPERTY_COMPONENT, old, this.component);
	}

	protected void setComponentContext()
	{
		if (component instanceof ContextAwareComponent)
		{
			ContextAwareComponent contextAwareComponent = (ContextAwareComponent) component;
			BaseComponentContext context = new BaseComponentContext();
			context.setComponentElement(this);
			contextAwareComponent.setContext(context);
		}
	}

	@Override
	public Object clone()
	{
		JRDesignComponentElement clone = (JRDesignComponentElement) super.clone();
		
		if (component instanceof JRCloneable)
		{
			clone.component = (Component) ((JRCloneable) component).clone();
			clone.setComponentContext();
		}
		else if (component != null)
		{
			if (log.isDebugEnabled())
			{
				log.debug("Component of type " + component.getClass().getName() 
						+ " does not implement JRCloneable, not cloning");
			}
		}
		
		clone.eventSupport = null;

		return clone;
	}

}
