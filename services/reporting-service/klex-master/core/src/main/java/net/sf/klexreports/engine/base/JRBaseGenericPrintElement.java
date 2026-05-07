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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRGenericElementType;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.PrintElementVisitor;

/**
 * A basic implementation of {@link JRGenericPrintElement}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseGenericPrintElement extends JRBasePrintElement 
		implements JRGenericPrintElement
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private JRGenericElementType genericType;
	private Map<String,Object> parameters = new LinkedHashMap<>();

	/**
	 * Creates a generic print element.
	 * 
	 * @param defaultStyleProvider the default style provider to use for the element
	 * @see KlexPrint#getDefaultStyleProvider()
	 */
	public JRBaseGenericPrintElement(JRDefaultStyleProvider defaultStyleProvider)
	{
		super(defaultStyleProvider);
	}

	@Override
	public JRGenericElementType getGenericType()
	{
		return genericType;
	}
	
	/**
	 * Sets the type of this element.
	 * 
	 * @param genericType the type of the element
	 * @see #getGenericType()
	 */
	public void setGenericType(JRGenericElementType genericType)
	{
		this.genericType = genericType;
	}

	@Override
	public Set<String> getParameterNames()
	{
		return parameters.keySet();
	}

	@Override
	public Object getParameterValue(String name)
	{
		return parameters.get(name);
	}

	@Override
	public boolean hasParameter(String name)
	{
		return parameters.containsKey(name);
	}

	@Override
	public void setParameterValue(String name, Object value)
	{
		parameters.put(name, value);
	}

	@Override
	public <T> void accept(PrintElementVisitor<T> visitor, T arg)
	{
		visitor.visit(this, arg);
	}

}
