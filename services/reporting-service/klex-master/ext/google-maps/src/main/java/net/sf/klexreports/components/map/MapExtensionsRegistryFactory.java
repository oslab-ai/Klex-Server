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
package net.sf.klexreports.components.map;

import java.util.HashMap;

import net.sf.klexreports.components.list.ListComponent;
import net.sf.klexreports.components.map.fill.MapFillFactory;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentManager;
import net.sf.klexreports.engine.component.ComponentsBundle;
import net.sf.klexreports.engine.component.DefaultComponentManager;
import net.sf.klexreports.engine.component.DefaultComponentsBundle;
import net.sf.klexreports.engine.export.GenericElementHandler;
import net.sf.klexreports.engine.export.GenericElementHandlerBundle;
import net.sf.klexreports.engine.export.HtmlExporter;
import net.sf.klexreports.engine.export.JRGraphics2DExporter;
import net.sf.klexreports.engine.export.JRRtfExporter;
import net.sf.klexreports.engine.export.oasis.JROdsExporter;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.export.ooxml.JRDocxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.xml.JRXmlConstants;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.ListExtensionsRegistry;
import net.sf.klexreports.json.export.JsonExporter;
import net.sf.klexreports.pdf.JRPdfExporter;
import net.sf.klexreports.poi.export.JRXlsExporter;

/**
 * Extension registry factory that includes built-in component element
 * implementations.
 * 
 * <p>
 * This registry factory is registered by default in KlexReports.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see ListComponent
 */
public class MapExtensionsRegistryFactory implements ExtensionsRegistryFactory
{
	
	public static final String MAP_COMPONENT_NAME = "map";
	
	private static final ExtensionsRegistry REGISTRY;
	
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
				if (MapComponent.MAP_ELEMENT_NAME.equals(elementName))
				{
					switch (exporterKey)
					{
						case JRGraphics2DExporter.GRAPHICS2D_EXPORTER_KEY:
						{
							return MapElementGraphics2DHandler.getInstance();
						}
						case HtmlExporter.HTML_EXPORTER_KEY:
						{
							return MapElementHtmlHandler.getInstance();
						}
						case JRPdfExporter.PDF_EXPORTER_KEY:
						{
							return MapElementPdfHandler.getInstance();
						}
						case JRXlsExporter.XLS_EXPORTER_KEY:
						{
							return MapElementXlsHandler.getInstance();
						}
						case JsonExporter.JSON_EXPORTER_KEY:
						{
							return MapElementJsonHandler.getInstance();
						}
						case JRXlsxExporter.XLSX_EXPORTER_KEY:
						{
							return MapElementXlsxHandler.getInstance();
						}
						case JRDocxExporter.DOCX_EXPORTER_KEY:
						{
							return MapElementDocxHandler.getInstance();
						}
						case JRPptxExporter.PPTX_EXPORTER_KEY:
						{
							return MapElementPptxHandler.getInstance();
						}
						case JRRtfExporter.RTF_EXPORTER_KEY:
						{
							return MapElementRtfHandler.getInstance();
						}
						case JROdtExporter.ODT_EXPORTER_KEY:
						{
							return MapElementOdtHandler.getInstance();
						}
						case JROdsExporter.ODS_EXPORTER_KEY:
						{
							return MapElementOdsHandler.getInstance();
						}
					}
				}
				
				return null;
			}
		};

	static
	{
		final DefaultComponentsBundle bundle = new DefaultComponentsBundle();
		
		HashMap<Class<? extends Component>, ComponentManager> componentManagers = new HashMap<>();
		
		DefaultComponentManager mapManager = new DefaultComponentManager();
		mapManager.setDesignConverter(MapDesignConverter.getInstance());
		mapManager.setComponentCompiler(new MapCompiler());
		mapManager.setComponentFillFactory(new MapFillFactory());
		componentManagers.put(MapComponent.class, mapManager);

		bundle.setComponentManagers(componentManagers);
		
		ListExtensionsRegistry registry = new ListExtensionsRegistry();
		registry.add(ComponentsBundle.class, bundle);
		registry.add(GenericElementHandlerBundle.class, HANDLER_BUNDLE);
		
		REGISTRY = registry;
	}
	
	@Override
	public ExtensionsRegistry createRegistry(String registryId,
			JRPropertiesMap properties)
	{
		return REGISTRY;
	}
}
