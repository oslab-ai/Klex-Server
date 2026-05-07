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

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRAbstractExporter;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.export.Exporter;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ElementPropertyExporterFilterFactory implements ExporterFilterFactory
{

	/**
	 * The suffix of element exclusion properties.
	 * 
	 * This suffix is appended to the exporter properties prefix, resulting
	 * in element exclusion properties such as
	 * <code>net.sf.klexreports.export.xls.exclude</code>. 
	 */
	@Property(
			name = "net.sf.klexreports.export.{format}.exclude",
			category = PropertyConstants.CATEGORY_EXPORT,
			scopes = {PropertyScope.ELEMENT},
			valueType = Boolean.class,
			sinceVersion = PropertyConstants.VERSION_6_11_0
			)
	public static final String PROPERTY_EXCLUDE_SUFFIX = "exclude";
	
	/**
	 * Default element exclusion property.
	 */
	@Property(
			name = "net.sf.klexreports.export.default.exclude",
			category = PropertyConstants.CATEGORY_EXPORT,
			scopes = {PropertyScope.ELEMENT},
			valueType = Boolean.class,
			defaultValue = "false",
			sinceVersion = PropertyConstants.VERSION_6_11_0
			)
	public static final String PROPERTY_DEFAULT_EXCLUDE = JRPropertiesUtil.PROPERTY_PREFIX + "export.default.exclude";

	@Override
	public ExporterFilter getFilter(JRExporterContext exporterContext)
			throws JRException
	{
		Exporter<?, ?, ?, ?> exporter = exporterContext.getExporterRef();
		if (!(exporter instanceof JRAbstractExporter<?, ?, ?, ?>))
		{
			return null;
		}
		
		String exporterPropertiesPrefix = ((JRAbstractExporter<?, ?, ?, ?>) exporter).getExporterPropertiesPrefix();
		String exportExcludeProperty = exporterPropertiesPrefix + PROPERTY_EXCLUDE_SUFFIX;
		ElementPropertyExporterFilter filter = new ElementPropertyExporterFilter(exportExcludeProperty);
		return filter;
	}

}
