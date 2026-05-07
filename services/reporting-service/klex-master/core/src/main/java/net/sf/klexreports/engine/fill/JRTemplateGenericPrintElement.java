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
package net.sf.klexreports.engine.fill;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRGenericElementType;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.PrintElementVisitor;
import net.sf.klexreports.engine.virtualization.VirtualizationInput;
import net.sf.klexreports.engine.virtualization.VirtualizationOutput;

/**
 * Implementation of {@link JRGenericPrintElement} that uses
 * a {@link JRTemplateGenericElement} instance to
 * store common attributes. 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see JRTemplateGenericPrintElement
 */
public class JRTemplateGenericPrintElement extends JRTemplatePrintElement
		implements JRGenericPrintElement
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private Map<String,Object> parameters;
	
	public JRTemplateGenericPrintElement()
	{
		
	}
	
	/**
	 * Creates a generic print element.
	 * 
	 * @param template the template to use for the element
	 * @param originator
	 */
	public JRTemplateGenericPrintElement(JRTemplateGenericElement template, PrintElementOriginator originator)
	{
		super(template, originator);
		
		parameters = new LinkedHashMap<>();
	}
	
	/**
	 * Creates a generic print element.
	 * 
	 * @param template the template to use for the element
	 * @param originator
	 * @param parameterCount the number of parameters that the element will have
	 */
	public JRTemplateGenericPrintElement(JRTemplateGenericElement template, PrintElementOriginator originator,
			int parameterCount)
	{
		super(template, originator);
		
		parameters = new LinkedHashMap<>(parameterCount * 4 / 3, 0.75f);
	}

	/**
	 * Returns the generic type specified by the element template.
	 * 
	 * @see JRTemplateGenericElement#getGenericType()
	 */
	@Override
	public JRGenericElementType getGenericType()
	{
		return ((JRTemplateGenericElement) template).getGenericType();
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

	@Override
	public void writeVirtualized(VirtualizationOutput out) throws IOException
	{
		super.writeVirtualized(out);
		
		out.writeIntCompressed(parameters.size());
		for (Entry<String, Object> entry : parameters.entrySet())
		{
			out.writeJRObject(entry.getKey());
			out.writeJRObject(entry.getValue());
		}
	}

	@Override
	public void readVirtualized(VirtualizationInput in) throws IOException
	{
		super.readVirtualized(in);
		
		int paramsCount = in.readIntCompressed();
		parameters = new LinkedHashMap<>(paramsCount * 4 / 3, 0.75f);
		for (int i = 0; i < paramsCount; i++)
		{
			String key = (String) in.readJRObject();
			Object value = in.readJRObject();
			parameters.put(key, value);
		}
	}

}
