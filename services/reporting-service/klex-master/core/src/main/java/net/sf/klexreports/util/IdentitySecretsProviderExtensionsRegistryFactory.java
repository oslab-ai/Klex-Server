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
package net.sf.klexreports.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.extensions.DefaultExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.SingletonExtensionRegistry;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class IdentitySecretsProviderExtensionsRegistryFactory implements ExtensionsRegistryFactory 
{
	/**
	 * 
	 */
	public final static String IDENTITY_SECTRETS_PROVIDER_CATEGORY_PROPERTY_PREFIX = DefaultExtensionsRegistry.PROPERTY_REGISTRY_PREFIX + "identity.secrets.category.";

	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) 
	{
		List<PropertySuffix> categoryProperties = JRPropertiesUtil.getProperties(properties, IDENTITY_SECTRETS_PROVIDER_CATEGORY_PROPERTY_PREFIX);
		Set<String> categories = new HashSet<>();
		for (Iterator<PropertySuffix> it = categoryProperties.iterator(); it.hasNext();) {
			PropertySuffix categoryProp = it.next();
			categories.add(categoryProp.getValue());
		}
		
		return new SingletonExtensionRegistry<SecretsProviderFactory>(SecretsProviderFactory.class, new IdentitySecretsProviderFactory(categories));
	}
}
