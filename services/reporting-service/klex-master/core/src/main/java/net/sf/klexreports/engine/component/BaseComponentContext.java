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
package net.sf.klexreports.engine.component;

import java.io.Serializable;

import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;

/**
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class BaseComponentContext implements ComponentContext, Serializable
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private JRComponentElement element;

	public BaseComponentContext()
	{
	}

	public BaseComponentContext(ComponentContext context, JRBaseObjectFactory objectFactory)
	{
		this.element = (JRComponentElement)objectFactory.getVisitResult(context.getComponentElement());
	}
	
	public void setComponentElement(JRComponentElement element)
	{
		this.element = element;
	}
	
	@Override
	public JRComponentElement getComponentElement()
	{
		return element;
	}
	
}
