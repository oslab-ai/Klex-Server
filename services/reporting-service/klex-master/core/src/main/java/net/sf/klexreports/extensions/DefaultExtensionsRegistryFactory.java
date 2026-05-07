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
package net.sf.klexreports.extensions;

import net.sf.klexreports.components.iconlabel.IconLabelElement;
import net.sf.klexreports.components.iconlabel.IconLabelElementCsvHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementDocxHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementGraphics2DHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementHtmlHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementOdsHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementOdtHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementPptxHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementRtfHandler;
import net.sf.klexreports.components.iconlabel.IconLabelElementXlsxHandler;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.export.GenericElementHandler;
import net.sf.klexreports.engine.export.GenericElementHandlerBundle;
import net.sf.klexreports.engine.export.HtmlExporter;
import net.sf.klexreports.engine.export.JRCsvExporter;
import net.sf.klexreports.engine.export.JRGraphics2DExporter;
import net.sf.klexreports.engine.export.JRRtfExporter;
import net.sf.klexreports.engine.export.oasis.JROdsExporter;
import net.sf.klexreports.engine.export.oasis.JROdtExporter;
import net.sf.klexreports.engine.export.ooxml.JRDocxExporter;
import net.sf.klexreports.engine.export.ooxml.JRPptxExporter;
import net.sf.klexreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.klexreports.engine.query.DefaultQueryExecuterFactoryBundle;
import net.sf.klexreports.engine.query.JRQueryExecuterFactoryBundle;
import net.sf.klexreports.engine.scriptlets.DefaultScriptletFactory;
import net.sf.klexreports.engine.scriptlets.ScriptletFactory;
import net.sf.klexreports.engine.util.MessageProviderFactory;
import net.sf.klexreports.engine.util.ResourceBundleMessageProviderFactory;
import net.sf.klexreports.engine.xml.JRXmlConstants;
import net.sf.klexreports.engine.xml.JacksonReportLoader;
import net.sf.klexreports.engine.xml.JacksonReportWriterFactory;
import net.sf.klexreports.engine.xml.ReportLoader;
import net.sf.klexreports.engine.xml.ReportWriterFactory;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class DefaultExtensionsRegistryFactory implements ExtensionsRegistryFactory
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
				if (IconLabelElement.ELEMENT_NAME.equals(elementName))
				{
					switch (exporterKey)
					{
						case JRGraphics2DExporter.GRAPHICS2D_EXPORTER_KEY :
						{
							return IconLabelElementGraphics2DHandler.getInstance();
						}
						case HtmlExporter.HTML_EXPORTER_KEY:
						{
							return IconLabelElementHtmlHandler.getInstance();
						}		
						case JRCsvExporter.CSV_EXPORTER_KEY:
						{
							return IconLabelElementCsvHandler.getInstance();
						}		
						case JRXlsxExporter.XLSX_EXPORTER_KEY:
						{
							return IconLabelElementXlsxHandler.getInstance();
						}		
						case JRDocxExporter.DOCX_EXPORTER_KEY:
						{
							return IconLabelElementDocxHandler.getInstance();
						}		
						case JRPptxExporter.PPTX_EXPORTER_KEY:
						{
							return IconLabelElementPptxHandler.getInstance();
						}		
						case JROdsExporter.ODS_EXPORTER_KEY:
						{
							return IconLabelElementOdsHandler.getInstance();
						}		
						case JROdtExporter.ODT_EXPORTER_KEY:
						{
							return IconLabelElementOdtHandler.getInstance();
						}		
						case JRRtfExporter.RTF_EXPORTER_KEY:
						{
							return IconLabelElementRtfHandler.getInstance();
						}		
	//					else if (JRXmlExporter.XML_EXPORTER_KEY.equals(exporterKey))
	//					{
	//						return IconLabelElementXmlHandler.getInstance();
	//					}
					}
				}
				
				return null;
			}
		};

	private static final ExtensionsRegistry REGISTRY; 

	static
	{
		ListExtensionsRegistry registry = new ListExtensionsRegistry();
		registry.add(JRQueryExecuterFactoryBundle.class, DefaultQueryExecuterFactoryBundle.getInstance());
		registry.add(ScriptletFactory.class, DefaultScriptletFactory.getInstance());
		registry.add(GenericElementHandlerBundle.class, HANDLER_BUNDLE);
		registry.add(MessageProviderFactory.class, ResourceBundleMessageProviderFactory.getInstance());
		registry.add(ReportLoader.class, JacksonReportLoader.instance());
		registry.add(ReportWriterFactory.class, JacksonReportWriterFactory.instance());
	
		REGISTRY = registry;
	}

	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) 
	{
		return REGISTRY;
	}
}
