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
package net.sf.klexreports.charts.util;

import java.util.Map;

import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.data.xy.XYDataset;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRPrintHyperlink;
import net.sf.klexreports.engine.util.Pair;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class XYChartHyperlinkProvider implements ChartHyperlinkProvider
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private Map<Comparable<?>, Map<Pair, JRPrintHyperlink>> itemHyperlinks;
	
	public XYChartHyperlinkProvider(Map<Comparable<?>, Map<Pair, JRPrintHyperlink>>  itemHyperlinks)
	{
		this.itemHyperlinks = itemHyperlinks;
	}


	@Override
	public JRPrintHyperlink getEntityHyperlink(ChartEntity entity)
	{
		JRPrintHyperlink printHyperlink = null;
		if (hasHyperlinks() && entity instanceof XYItemEntity)
		{
			XYItemEntity itemEntity = (XYItemEntity) entity;
			XYDataset dataset = itemEntity.getDataset();
			Comparable<?> serie = dataset.getSeriesKey(itemEntity.getSeriesIndex());
			Map<Pair, JRPrintHyperlink> serieHyperlinks = itemHyperlinks.get(serie);
			if (serieHyperlinks != null)
			{
				Number x = dataset.getX(itemEntity.getSeriesIndex(), itemEntity.getItem());
				Number y = dataset.getY(itemEntity.getSeriesIndex(), itemEntity.getItem());
				Pair<Number,Number> xyKey = new Pair<>(x, y);
				printHyperlink = serieHyperlinks.get(xyKey);
			}
		}
		return printHyperlink;
	}

	@Override
	public boolean hasHyperlinks()
	{
		return itemHyperlinks != null && itemHyperlinks.size() > 0;
	}
}
