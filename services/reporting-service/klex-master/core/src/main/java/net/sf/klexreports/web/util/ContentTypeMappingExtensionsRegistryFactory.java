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
package net.sf.klexreports.web.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.extensions.DefaultExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.ListExtensionRegistry;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class ContentTypeMappingExtensionsRegistryFactory implements ExtensionsRegistryFactory {

	/**
	 * 
	 */
	public final static String CONTENT_TYPE_MAPPING_PROPERTY_PREFIX = DefaultExtensionsRegistry.PROPERTY_REGISTRY_PREFIX + "content.type.mapping.";

	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) {
		List<PropertySuffix> contentTypeMappingProperties = JRPropertiesUtil.getProperties(properties, CONTENT_TYPE_MAPPING_PROPERTY_PREFIX);
		List<ContentTypeMapping> contentTypeMappings = new ArrayList<>();
		for (Iterator<PropertySuffix> it = contentTypeMappingProperties.iterator(); it.hasNext();) {
			PropertySuffix contentTypeMappingProp = it.next();
			contentTypeMappings.add(new ContentTypeMapping(contentTypeMappingProp.getSuffix(),contentTypeMappingProp.getValue()));
		}
		
		return new ListExtensionRegistry<ContentTypeMapping>(ContentTypeMapping.class, contentTypeMappings);
	}

}
