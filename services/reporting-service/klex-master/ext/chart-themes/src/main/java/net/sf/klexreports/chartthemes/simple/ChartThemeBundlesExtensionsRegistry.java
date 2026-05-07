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
package net.sf.klexreports.chartthemes.simple;

import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.charts.ChartThemeBundle;
import net.sf.klexreports.extensions.ExtensionsRegistry;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ChartThemeBundlesExtensionsRegistry implements ExtensionsRegistry
{

	private final List<ChartThemeBundle> chartThemeBundles;
	
	public ChartThemeBundlesExtensionsRegistry(List<ChartThemeBundle> chartThemeBundles)
	{
		this.chartThemeBundles = chartThemeBundles;
	}
	
	public ChartThemeBundlesExtensionsRegistry(ChartThemeBundle chartThemeBundle)
	{
		this.chartThemeBundles = new ArrayList<>(1);
		this.chartThemeBundles.add(chartThemeBundle);
	}
	
	@Override
	public <T> List<T> getExtensions(Class<T> extensionType)
	{
		if (ChartThemeBundle.class.equals(extensionType)) 
		{
			@SuppressWarnings("unchecked")
			List<T> extensions = (List<T>)chartThemeBundles;
			return extensions;
		}
		return null;
	}

}
