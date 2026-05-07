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
package net.sf.klexreports.engine.export;

import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ElementPropertyExporterFilter implements ExporterFilter
{
	
	private String exportExcludeProperty;

	public ElementPropertyExporterFilter(String exportExcludeProperty)
	{
		this.exportExcludeProperty = exportExcludeProperty;
	}

	@Override
	public boolean isToExport(JRPrintElement element)
	{
		if (!(element.hasProperties()))
		{
			return true;
		}
		
		JRPropertiesMap properties = element.getPropertiesMap();
		String exclude = properties.getProperty(exportExcludeProperty);
		if (exclude != null)
		{
			return !JRPropertiesUtil.asBoolean(exclude);
		}
		
		String defaultExclude = properties.getProperty(ElementPropertyExporterFilterFactory.PROPERTY_DEFAULT_EXCLUDE);
		if (defaultExclude != null)
		{
			return !JRPropertiesUtil.asBoolean(defaultExclude);
		}
		
		return true;
	}

}
