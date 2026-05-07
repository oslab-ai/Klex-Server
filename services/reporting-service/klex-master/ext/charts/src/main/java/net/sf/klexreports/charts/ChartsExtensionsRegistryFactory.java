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
package net.sf.klexreports.charts;

import java.util.HashMap;

import net.sf.klexreports.charts.fill.DefaultChartTheme;
import net.sf.klexreports.components.spiderchart.SpiderChartCompiler;
import net.sf.klexreports.components.spiderchart.SpiderChartComponent;
import net.sf.klexreports.components.spiderchart.SpiderChartDesignConverter;
import net.sf.klexreports.components.spiderchart.SpiderChartFillFactory;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentManager;
import net.sf.klexreports.engine.component.ComponentsBundle;
import net.sf.klexreports.engine.component.DefaultComponentManager;
import net.sf.klexreports.engine.component.DefaultComponentsBundle;
import net.sf.klexreports.extensions.ExtensionsRegistry;
import net.sf.klexreports.extensions.ExtensionsRegistryFactory;
import net.sf.klexreports.extensions.ListExtensionsRegistry;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ChartsExtensionsRegistryFactory implements ExtensionsRegistryFactory
{
	
	public static final String SPIDERCHART_COMPONENT_NAME = "spiderChart";
	
	private static final ExtensionsRegistry REGISTRY;
	
	static
	{
		final DefaultComponentsBundle bundle = new DefaultComponentsBundle();
		
		HashMap<Class<? extends Component>, ComponentManager> componentManagers = new HashMap<>();
		
		DefaultComponentManager spiderChartManager = new DefaultComponentManager();
		spiderChartManager.setDesignConverter(new SpiderChartDesignConverter());
		spiderChartManager.setComponentCompiler(new SpiderChartCompiler());
		spiderChartManager.setComponentFillFactory(new SpiderChartFillFactory());
		componentManagers.put(SpiderChartComponent.class, spiderChartManager);
		
		bundle.setComponentManagers(componentManagers);
		
		ListExtensionsRegistry registry = new ListExtensionsRegistry();
		registry.add(ComponentsBundle.class, bundle);
		registry.add(ChartThemeBundle.class, DefaultChartTheme.BUNDLE);
		registry.add(ElementVisitorAdapter.class, DefaultElementVisitorsAdapter.instance());
		
		REGISTRY = registry;
	}
	
	@Override
	public ExtensionsRegistry createRegistry(String registryId,
			JRPropertiesMap properties)
	{
		return REGISTRY;
	}

}
