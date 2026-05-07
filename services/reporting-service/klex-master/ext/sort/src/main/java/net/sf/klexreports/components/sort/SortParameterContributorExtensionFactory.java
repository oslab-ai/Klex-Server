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
package net.sf.klexreports.components.sort;

import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.ParameterContributorFactory;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.SingletonExtensionRegistry;

/**
 * Extension factory for {@link SortParameterContributorFactory}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SortParameterContributorExtensionFactory implements ExtensionsRegistryFactory
{
	
	private static final ExtensionsRegistry REGISTRY = 
			new SingletonExtensionRegistry<ParameterContributorFactory>(ParameterContributorFactory.class, 
					SortParameterContributorFactory.getInstance());

	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties)
	{
		return REGISTRY;
	}

}
