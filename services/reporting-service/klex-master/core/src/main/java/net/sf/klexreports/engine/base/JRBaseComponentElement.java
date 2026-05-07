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

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.JRVisitable;
import net.sf.klexreports.engine.JRVisitor;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentManager;
import net.sf.klexreports.engine.component.ComponentsEnvironment;

/**
 * A read-only {@link JRComponentElement} implementation which is included
 * in compiled reports.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseComponentElement extends JRBaseElement implements
		JRComponentElement
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private Component component;
	
	public JRBaseComponentElement(JRComponentElement element, JRBaseObjectFactory factory)
	{
		super(element, factory);
		
		Component elementComponent = element.getComponent();
		ComponentManager manager = ComponentsEnvironment.getInstance(DefaultKlexReportsContext.getInstance()).getManager(elementComponent);
		component = manager.getComponentCompiler(DefaultKlexReportsContext.getInstance()).toCompiledComponent(
				elementComponent, factory);
	}

	@Override
	public Component getComponent()
	{
		return component;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		ComponentManager manager = ComponentsEnvironment.getInstance(DefaultKlexReportsContext.getInstance()).getManager(component);
		manager.getComponentCompiler(DefaultKlexReportsContext.getInstance()).collectExpressions(component, collector);
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

}
