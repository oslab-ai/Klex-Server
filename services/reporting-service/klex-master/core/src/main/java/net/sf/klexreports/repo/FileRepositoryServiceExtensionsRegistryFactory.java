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
package net.sf.klexreports.repo;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.extensions.DefaultExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.SingletonExtensionRegistry;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class FileRepositoryServiceExtensionsRegistryFactory implements ExtensionsRegistryFactory
{

	/**
	 * 
	 */
	public final static String FILE_REPOSITORY_PROPERTY_PREFIX = 
		DefaultExtensionsRegistry.PROPERTY_REGISTRY_PREFIX + "file.repository.";
	
	/**
	 * Specifies the file repository root location.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_REPOSITORY,
			scopes = {PropertyScope.GLOBAL, PropertyScope.EXTENSION},
			sinceVersion = PropertyConstants.VERSION_4_1_1
			)
	public final static String PROPERTY_FILE_REPOSITORY_ROOT = FILE_REPOSITORY_PROPERTY_PREFIX + "root";
	
	/**
	 * Flag property that indicates whether the absolute path to be used instead, when resources are not found in the file repository.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_REPOSITORY,
			defaultValue = PropertyConstants.BOOLEAN_FALSE,
			scopes = {PropertyScope.GLOBAL, PropertyScope.EXTENSION},
			sinceVersion = PropertyConstants.VERSION_4_1_1,
			valueType = Boolean.class
			)
	public final static String PROPERTY_FILE_REPOSITORY_RESOLVE_ABSOLUTE_PATH = FILE_REPOSITORY_PROPERTY_PREFIX + "resolve.absolute.path";
	
	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) 
	{
		String root = JRPropertiesUtil.getInstance(DefaultKlexReportsContext.getInstance()).getProperty(properties, PROPERTY_FILE_REPOSITORY_ROOT);
		boolean resolveAbsolutePath = JRPropertiesUtil.getInstance(DefaultKlexReportsContext.getInstance()).getBooleanProperty(properties, PROPERTY_FILE_REPOSITORY_RESOLVE_ABSOLUTE_PATH, false);

		return new SingletonExtensionRegistry<RepositoryService>(RepositoryService.class, new FileRepositoryService(DefaultKlexReportsContext.getInstance(), root, resolveAbsolutePath));
	}
}
