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
package net.sf.klexreports.poi;

import net.sf.klexreports.components.iconlabel.IconLabelElement;
import net.sf.klexreports.dataadapters.DataAdapterContributorFactory;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.export.GenericElementHandler;
import net.sf.klexreports.engine.export.GenericElementHandlerBundle;
import net.sf.klexreports.engine.query.JRQueryExecuterFactoryBundle;
import net.sf.klexreports.engine.xml.JRXmlConstants;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.ListExtensionsRegistry;
import net.sf.klexreports.poi.components.iconlabel.IconLabelElementXlsHandler;
import net.sf.klexreports.poi.data.PoiDataAdapterServiceFactory;
import net.sf.klexreports.poi.export.JRXlsExporter;
import net.sf.klexreports.poi.query.PoiQueryExecuterFactoryBundle;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PoiExtensionsRegistryFactory implements ExtensionsRegistryFactory
{
	private static final GenericElementHandlerBundle HANDLER_BUNDLE = 
		new GenericElementHandlerBundle()
		{
			@Override
			public String getNamespace()
			{
				return JRXmlConstants.KLEXREPORTS_NAMESPACE;
			}
			
			@Override
			public GenericElementHandler getHandler(String elementName, String exporterKey)
			{
				if (IconLabelElement.ELEMENT_NAME.equals(elementName))
				{
					if (JRXlsExporter.XLS_EXPORTER_KEY.equals(exporterKey))
					{
						return IconLabelElementXlsHandler.getInstance();
					}		
				}
				
				return null;
			}
		};

	private static final ExtensionsRegistry REGISTRY; 

	static
	{
		ListExtensionsRegistry registry = new ListExtensionsRegistry();
		registry.add(JRQueryExecuterFactoryBundle.class, PoiQueryExecuterFactoryBundle.getInstance());
		registry.add(DataAdapterContributorFactory.class, PoiDataAdapterServiceFactory.getInstance());
		registry.add(GenericElementHandlerBundle.class, HANDLER_BUNDLE);
		
		REGISTRY = registry;
	}
	
	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) 
	{
		return REGISTRY;
	}
}
