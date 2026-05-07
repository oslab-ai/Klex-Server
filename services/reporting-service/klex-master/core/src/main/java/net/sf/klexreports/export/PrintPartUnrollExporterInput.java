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
package net.sf.klexreports.export;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.klexreports.engine.JRAbstractExporter;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.PrintPart;
import net.sf.klexreports.engine.PrintParts;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PrintPartUnrollExporterInput implements ExporterInput
{
	private Class<? extends ReportExportConfiguration> itemConfigurationInterface;
	private List<ExporterInputItem> partItems;
	
	/**
	 * 
	 */
	public PrintPartUnrollExporterInput(ExporterInput exporterInput, Class<? extends ReportExportConfiguration> itemConfigurationInterface)
	{
		this.itemConfigurationInterface = itemConfigurationInterface;
		partItems = new ArrayList<>();
		
		for (ExporterInputItem item : exporterInput.getItems())
		{
			KlexPrint klexPrint = item.getKlexPrint();
			//SortedMap<Integer, PrintPart> parts = klexPrint.getParts();
			if (klexPrint.hasParts())
			{
				//exporting each part to a separate sheet irrespective of the visibility flag
				PrintParts parts = klexPrint.getParts();
				Iterator<Map.Entry<Integer, PrintPart>> it = parts.partsIterator();
				Map.Entry<Integer, PrintPart> part = it.next();
				while (it.hasNext())
				{
					Map.Entry<Integer, PrintPart> next = it.next();
					addPartItem(item, part.getValue(), part.getKey(), next.getKey());
					part = next;
				}
				
				addPartItem(item, part.getValue(), part.getKey(), klexPrint.getPages().size());
			}
			else
			{
				partItems.add(item);
			}
		}
	}

	private void addPartItem(ExporterInputItem item, PrintPart part, int partStartIndex, int partEndIndex)
	{
		KlexPrint klexPrint = item.getKlexPrint();
		ReportExportConfiguration configuration = item.getConfiguration();
		Integer configStartPageIndex = null;
		Integer configEndPageIndex = null;
		if (configuration != null)
		{
			Integer configPageIndex = configuration.getPageIndex();
			configStartPageIndex = configPageIndex == null ? configuration.getStartPageIndex() : configPageIndex;
			configEndPageIndex = configPageIndex == null ? configuration.getEndPageIndex() : configPageIndex;
		}

		int itemStartIndex = configStartPageIndex == null ? partStartIndex : Math.max(configStartPageIndex, partStartIndex);
		int itemEndIndex = configStartPageIndex == null ? partEndIndex: Math.min(configEndPageIndex + 1, partEndIndex);
		if (itemStartIndex < itemEndIndex)
		{
			ReadOnlyPartKlexPrint itemKlexPrint = new ReadOnlyPartKlexPrint(klexPrint, part, 
					partStartIndex, partEndIndex);
			
			ReportExportConfiguration itemConfiguration;
			if ((configStartPageIndex == null || configStartPageIndex == itemStartIndex - partStartIndex)
					&& (configEndPageIndex == null || configEndPageIndex == itemEndIndex - partStartIndex - 1))
			{
				itemConfiguration = configuration;
			}
			else
			{
				itemConfiguration = overrideConfiguration(configuration, 
						itemStartIndex - partStartIndex, itemEndIndex - partStartIndex - 1);
			}
			
			partItems.add(new SimpleExporterInputItem(itemKlexPrint, itemConfiguration));
		}
	}

	private ReportExportConfiguration overrideConfiguration(ReportExportConfiguration configuration, 
			int startIndex, int endIndex)
	{
		return (ReportExportConfiguration) Proxy.newProxyInstance(
				JRAbstractExporter.class.getClassLoader(),
				new Class<?>[] {itemConfigurationInterface},
				new InvocationHandler()
				{
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
					{
						if (method.getName().equals("getPageIndex"))
						{
							return null;
						}
						if (method.getName().equals("getStartPageIndex"))
						{
							return startIndex;
						}
						if (method.getName().equals("getEndPageIndex"))
						{
							return endIndex;
						}
						return method.invoke(configuration, args);
					}
				});
	}

	@Override
	public List<ExporterInputItem> getItems()
	{
		return partItems;
	}
}