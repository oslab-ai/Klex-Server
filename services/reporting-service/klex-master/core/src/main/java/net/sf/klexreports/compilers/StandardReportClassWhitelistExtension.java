/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.compilers;

import java.util.List;

import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.SingletonExtensionRegistry;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class StandardReportClassWhitelistExtension implements ExtensionsRegistryFactory
{

	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties)
	{
		StandardReportClassWhitelist whitelist = new StandardReportClassWhitelist();
		List<PropertySuffix> whitelistProps = JRPropertiesUtil.getProperties(properties, 
				ReportClassFilter.PROPERTY_PREFIX_CLASS_WHITELIST);
		for (PropertySuffix propertySuffix : whitelistProps)
		{
			whitelist.addWhitelist(propertySuffix.getValue());
		}
		return new SingletonExtensionRegistry<>(ReportClassWhitelist.class, whitelist);
	}

}
