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
import java.util.List;
import java.util.Map;

import net.sf.klexreports.components.items.Item;
import net.sf.klexreports.components.items.ItemCompiler;
import net.sf.klexreports.components.items.ItemData;
import net.sf.klexreports.components.items.ItemProperty;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentCompiler;
import net.sf.klexreports.engine.design.JRVerifier;
import net.sf.klexreports.engine.type.EvaluationTimeEnum;

/**
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class MapCompiler implements ComponentCompiler
{
	
	private final static Map<String,String> addressMap = new HashMap<>();
	static {
		addressMap.put(MapComponent.ITEM_PROPERTY_latitude, MapComponent.ITEM_PROPERTY_address);
		addressMap.put(MapComponent.ITEM_PROPERTY_longitude, MapComponent.ITEM_PROPERTY_address);
	}
	
	@Override
	public void collectExpressions(Component component, JRExpressionCollector collector)
	{
		MapComponent map = (MapComponent) component;
		collector.addExpression(map.getLatitudeExpression());
		collector.addExpression(map.getLongitudeExpression());
		collector.addExpression(map.getAddressExpression());
		collector.addExpression(map.getZoomExpression());
		collector.addExpression(map.getLanguageExpression());

		collectExpressions(map.getLegendItem(), collector);
		collectExpressions(map.getResetMapItem(), collector);

		List<MarkerItemData> markerItemDataList = map.getMarkerItemDataList();
		if(markerItemDataList != null && markerItemDataList.size() > 0) {
			for(MarkerItemData markerData : markerItemDataList){
				ItemCompiler.collectExpressions(markerData, collector);
				collector.addExpression(markerData.getSeriesNameExpression());
				collector.addExpression(markerData.getMarkerClusteringExpression());
				collector.addExpression(markerData.getMarkerSpideringExpression());
				collector.addExpression(markerData.getLegendIconExpression());
			}
		}
		List<ItemData> pathStyleList = map.getPathStyleList();
		if(pathStyleList != null && pathStyleList.size() > 0) {
			for(ItemData pathStyle : pathStyleList){
				ItemCompiler.collectExpressions(pathStyle, collector);
			}
		}
		List<ItemData> pathDataList = map.getPathDataList();
		if(pathDataList != null && pathDataList.size() > 0) {
			for(ItemData pathData : pathDataList){
				ItemCompiler.collectExpressions(pathData, collector);
			}
		}
	}

	protected void collectExpressions(Item item, JRExpressionCollector collector)
	{
		if (item != null)
		{
			List<ItemProperty> itemProperties = item.getProperties();
			if(itemProperties != null)
			{
				for(ItemProperty property : itemProperties)
				{
					collector.addExpression(property.getValueExpression());
				}
			}
		}
	}

	@Override
	public Component toCompiledComponent(Component component,
			JRBaseObjectFactory baseFactory)
	{
		MapComponent map = (MapComponent) component;
		return new StandardMapComponent(map, baseFactory);
	}

	@Override
	public void verify(Component component, JRVerifier verifier)
	{
		MapComponent map = (MapComponent) component;
		
		EvaluationTimeEnum evaluationTime = map.getEvaluationTime();
		if (evaluationTime == EvaluationTimeEnum.AUTO)
		{
			verifier.addBrokenRule("Auto evaluation time is not supported for maps", map);
		}
		else if (evaluationTime == EvaluationTimeEnum.GROUP)
		{
			String evaluationGroup = map.getEvaluationGroup();
			if (evaluationGroup == null || evaluationGroup.length() == 0)
			{
				verifier.addBrokenRule("No evaluation group set for map", map);
			}
			else if (!verifier.getReportDesign().getGroupsMap().containsKey(evaluationGroup))
			{
				verifier.addBrokenRule("Map evaluation group \"" 
						+ evaluationGroup + " not found", map);
			}
		}
		
		if((map.getLatitudeExpression() == null || map.getLongitudeExpression() == null) && map.getAddressExpression() == null){
			verifier.addBrokenRule("Missing the latitude and/or the longitude expression for the map center. Try to configure them properly, or configure the equivalent addressExpression for this map.", map);
		}
		
		String[] reqNames = new String[]{MapComponent.ITEM_PROPERTY_latitude, MapComponent.ITEM_PROPERTY_longitude};
		List<MarkerItemData> markerItemDataList = map.getMarkerItemDataList();
		if (markerItemDataList != null && markerItemDataList.size() > 0)
		{
			for(ItemData markerData : markerItemDataList){
				ItemCompiler.verifyItemData(verifier, markerData, MapComponent.ELEMENT_MARKER_DATA, reqNames, addressMap);
			}
		}

		List<ItemData> pathStyleList = map.getPathStyleList();
		if (pathStyleList != null && pathStyleList.size() > 0)
		{
			for(ItemData pathStyle : pathStyleList){
				ItemCompiler.verifyItemData(verifier, pathStyle, MapComponent.ELEMENT_PATH_STYLE, new String[]{MapComponent.ITEM_PROPERTY_name}, null);
			}
		}
		
		List<ItemData> pathDataList = map.getPathDataList();
		if (pathDataList != null && pathDataList.size() > 0)
		{
			for(ItemData pathData : pathDataList){
				ItemCompiler.verifyItemData(verifier, pathData, MapComponent.ELEMENT_PATH_DATA, reqNames, addressMap);
			}
		}
	}
	
}
