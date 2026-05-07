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

import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.json.export.GenericElementJsonHandler;
import net.sf.klexreports.json.export.JsonExporterContext;
import net.sf.klexreports.velocity.util.VelocityUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class MapElementJsonHandler implements GenericElementJsonHandler
{
	private static final MapElementJsonHandler INSTANCE = new MapElementJsonHandler();

	private static final String MAP_ELEMENT_JSON_TEMPLATE = "net/sf/klexreports/components/map/resources/templates/MapElementJsonTemplate.vm";

	public static MapElementJsonHandler getInstance()
	{
		return INSTANCE;
	}

	@Override
	public String getJsonFragment(JsonExporterContext context, JRGenericPrintElement element)
	{
		Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("mapCanvasId", "map_canvas_" + element.hashCode());

        MapUtils.prepareContextForVelocityTemplate(contextMap, context.getKlexReportsContext(), element);

		return VelocityUtil.processTemplate(MAP_ELEMENT_JSON_TEMPLATE, contextMap);
	}

	@Override
	public boolean toExport(JRGenericPrintElement element)
    {
		return true;
	}

}
