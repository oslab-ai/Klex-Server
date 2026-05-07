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
package net.sf.klexreports.interactivity;

import net.sf.klexreports.components.headertoolbar.HeaderToolbarElement;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.export.GenericElementHandler;
import net.sf.klexreports.engine.export.GenericElementHandlerBundle;
import net.sf.klexreports.engine.export.HtmlExporter;
import net.sf.klexreports.engine.fill.JRFillCrosstab;
import net.sf.klexreports.engine.xml.JRXmlConstants;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.SingletonExtensionRegistry;
import net.sf.klexreports.interactivity.crosstabs.CrosstabInteractiveJsonHandler;
import net.sf.klexreports.interactivity.headertoolbar.json.HeaderToolbarElementJsonHandler;
import net.sf.klexreports.interactivity.sort.SortElement;
import net.sf.klexreports.interactivity.sort.SortElementHtmlHandler;
import net.sf.klexreports.interactivity.sort.SortElementJsonHandler;
import net.sf.klexreports.json.export.JsonExporter;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class InteractivityExtensionsRegistryFactory implements ExtensionsRegistryFactory
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
			public GenericElementHandler getHandler(String elementName,
					String exporterKey)
			{
				switch (elementName)
				{
					case SortElement.SORT_ELEMENT_NAME:
					{
						if (HtmlExporter.HTML_EXPORTER_KEY.equals(exporterKey))
						{
							return SortElementHtmlHandler.getInstance();
						}
						else if (JsonExporter.JSON_EXPORTER_KEY.equals(exporterKey))
						{
							return new SortElementJsonHandler();
						}
					}
					case HeaderToolbarElement.ELEMENT_NAME:
					{
						if (JsonExporter.JSON_EXPORTER_KEY.equals(exporterKey))
						{
							return HeaderToolbarElementJsonHandler.getInstance();
						}
					}
					case JRFillCrosstab.CROSSTAB_INTERACTIVE_ELEMENT_NAME:
					{
						if (JsonExporter.JSON_EXPORTER_KEY.equals(exporterKey))
						{
							return CrosstabInteractiveJsonHandler.getInstance();
						}
					}
				}
				
				return null;
			}
		};

	private static final ExtensionsRegistry REGISTRY = new SingletonExtensionRegistry<>(GenericElementHandlerBundle.class, HANDLER_BUNDLE);

	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) 
	{
		return REGISTRY;
	}
}
