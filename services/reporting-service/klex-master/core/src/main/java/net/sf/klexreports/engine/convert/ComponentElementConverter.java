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

/*
 * Contributors:
 * Eugene D - eugenedruy@users.sourceforge.net 
 * Adrian Jackson - iapetus@users.sourceforge.net
 * David Taylor - exodussystems@users.sourceforge.net
 * Lars Kristensen - llk@users.sourceforge.net
 */
package net.sf.klexreports.engine.convert;

import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentDesignConverter;
import net.sf.klexreports.engine.component.ComponentManager;
import net.sf.klexreports.engine.util.JRImageLoader;


/**
 * Converter of {@link JRComponentElement} into print elements.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public final class ComponentElementConverter extends ElementConverter
{
	
	private final static ComponentElementConverter INSTANCE = new ComponentElementConverter();

	/**
	 * Returns the singleton instance of this converter.
	 * 
	 * @return the singleton component converter instance 
	 */
	public static ComponentElementConverter getInstance()
	{
		return INSTANCE;
	}
	
	private final static ElementIconConverter ICON_CONVERTER = new ElementIconConverter(
			JRImageLoader.COMPONENT_IMAGE_RESOURCE);
	
	private ComponentElementConverter()
	{
	}

	@Override
	public JRPrintElement convert(ReportConverter reportConverter,
			JRElement element)
	{
		JRComponentElement componentElement = (JRComponentElement) element;
		JRPrintElement converted = null;
		Component component = componentElement.getComponent();
		if (component != null)
		{
			ComponentManager manager = reportConverter.getComponentsEnvironment().getManager(component);
			if (manager != null)
			{
				ComponentDesignConverter converter = manager.getDesignConverter(reportConverter.getKlexReportsContext());
				if (converter != null)
				{
					// convert using the component converter
					converted = converter.convert(reportConverter, componentElement);
				}
			}
		}
		
		if (converted == null)
		{
			// fallback to the icon converter
			converted = ICON_CONVERTER.convert(reportConverter, element);
		}
		
		return converted;
	}

}
