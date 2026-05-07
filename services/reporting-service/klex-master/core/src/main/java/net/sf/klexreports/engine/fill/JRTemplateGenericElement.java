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

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.JRGenericElement;
import net.sf.klexreports.engine.JRGenericElementType;
import net.sf.klexreports.engine.JROrigin;
import net.sf.klexreports.engine.util.ObjectUtils;

/**
 * Generic print element information shared by multiple elements.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see JRTemplateGenericPrintElement
 */
public class JRTemplateGenericElement extends JRTemplateElement
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private JRGenericElementType genericType;

	protected JRTemplateGenericElement(JROrigin origin, 
			JRDefaultStyleProvider defaultStyleProvider, JRGenericElement element)
	{
		super(origin, defaultStyleProvider);
		
		setElement(element);
		setGenericType(element.getGenericType());
	}

	/**
	 * Creates a generic print element template.
	 * 
	 * @param origin the origin of the elements that will use the template
	 * @param defaultStyleProvider the style provider to be used for the elements
	 * @param genericType the type of the generic elements
	 */
	public JRTemplateGenericElement(JROrigin origin, 
			JRDefaultStyleProvider defaultStyleProvider,
			JRGenericElementType genericType)
	{
		super(origin, defaultStyleProvider);
		
		setGenericType(genericType);
	}

	/**
	 * Creates a generic print element template.
	 * 
	 * @param origin the origin of the elements that will use the template
	 * @param defaultStyleProvider the style provider to be used for the elements
	 * @param element an element to copy basic elements from
	 * @param genericType the type of the generic elements
	 */
	public JRTemplateGenericElement(JROrigin origin, 
			JRDefaultStyleProvider defaultStyleProvider,
			JRElement element,
			JRGenericElementType genericType)
	{
		super(origin, defaultStyleProvider);
		
		setElement(element);
		setGenericType(genericType);
	}
	
	/**
	 * Returns the type of the generic elements that use this template.
	 * 
	 * @return the type of the generic elements
	 */
	public JRGenericElementType getGenericType()
	{
		return genericType;
	}
	
	/**
	 * Sets the type of the generic elements that use this template.
	 * 
	 * @param genericType the generic type
	 */
	public void setGenericType(JRGenericElementType genericType)
	{
		this.genericType = genericType;
	}

	@Override
	public int getHashCode()
	{
		ObjectUtils.HashCode hash = ObjectUtils.hash();
		addTemplateHash(hash);
		hash.add(genericType);
		return hash.getHashCode();
	}

	@Override
	public boolean isIdentical(Object object)
	{
		if (this == object)
		{
			return true;
		}
		
		if (!(object instanceof JRTemplateGenericElement))
		{
			return false;
		}
		
		JRTemplateGenericElement template = (JRTemplateGenericElement) object;
		return templateIdentical(template)
				&& ObjectUtils.equals(genericType, template.genericType);
	}

}
