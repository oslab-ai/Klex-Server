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
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.jackson.util.JacksonUtil;

import java.util.List;
import java.util.Map;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class MapUtils {
    public static final String MAP_API_SCRIPT = "net/sf/klexreports/components/map/resources/scripts/klexreportsMapApi.js";

    public static void prepareContextForVelocityTemplate(
            Map<String, Object> velocityContext,
            KlexReportsContext klexReportsContext,
            JRGenericPrintElement element) {

        JacksonUtil jacksonUtil = JacksonUtil.getInstance(klexReportsContext);

        Float latitude = (Float) element.getParameterValue(MapComponent.ITEM_PROPERTY_latitude);
        latitude = latitude == null ? MapComponent.DEFAULT_LATITUDE : latitude;

        Float longitude = (Float) element.getParameterValue(MapComponent.ITEM_PROPERTY_longitude);
        longitude = longitude == null ? MapComponent.DEFAULT_LONGITUDE : longitude;

        Integer zoom = (Integer) element.getParameterValue(MapComponent.PARAMETER_ZOOM);
        zoom = zoom == null ? MapComponent.DEFAULT_ZOOM : zoom;

        String mapType = (String) element.getParameterValue(MapComponent.ATTRIBUTE_MAP_TYPE);
        mapType = (mapType == null ? MapComponent.DEFAULT_MAP_TYPE.getName() : mapType).toUpperCase();

        Boolean markerClustering = (Boolean) element.getParameterValue(MapComponent.ATTRIBUTE_MARKER_CLUSTERING);
        markerClustering = markerClustering != null ? markerClustering.booleanValue() : false;

        Boolean markerSpidering = (Boolean) element.getParameterValue(MapComponent.ATTRIBUTE_MARKER_SPIDERING);
        markerSpidering = markerSpidering != null ? markerSpidering.booleanValue() : false;

        velocityContext.put("latitude", latitude);
        velocityContext.put("longitude", longitude);
        velocityContext.put("zoom", zoom);
        velocityContext.put("mapType", mapType);
        velocityContext.put("useMarkerClustering", markerClustering);
        velocityContext.put("useMarkerSpidering", markerSpidering);

        Map<String, Object> legendPropertiesParam = (Map<String,Object>) element.getParameterValue(MapComponent.PARAMETER_LEGEND_PROPERTIES);
        String legendProperties = legendPropertiesParam == null ? "{}" : jacksonUtil.getIndentedJsonString(legendPropertiesParam);
        velocityContext.put("legendProperties", legendProperties);

        Map<String, Object> resetMapPropertiesParam = (Map<String,Object>) element.getParameterValue(MapComponent.PARAMETER_RESET_MAP_PROPERTIES);
        String resetMapProperties = resetMapPropertiesParam == null ? "{}" : jacksonUtil.getIndentedJsonString(resetMapPropertiesParam);
        velocityContext.put("resetMapProperties", resetMapProperties);

        Map<String, Object> markerSeries = (Map<String,Object>) element.getParameterValue(MapComponent.PARAMETER_MARKERS);
        String markers = markerSeries == null || markerSeries.isEmpty() ? "{}" : jacksonUtil.getIndentedJsonString(markerSeries);
        velocityContext.put("markerList", markers);

        List<Map<String,Object>> pathList = (List<Map<String,Object>>) element.getParameterValue(MapComponent.PARAMETER_PATHS);
        String paths = pathList == null || pathList.isEmpty() ? "[]" : jacksonUtil.getIndentedJsonString(pathList);
        velocityContext.put("pathsList", paths);

        String reqParams = (String)element.getParameterValue(MapComponent.PARAMETER_REQ_PARAMS);
        if (reqParams != null) {
            velocityContext.put(MapComponent.PARAMETER_REQ_PARAMS, reqParams);
        }

        String defaultMarkerIcon = (String)element.getParameterValue(MapComponent.PARAMETER_DEFAULT_MARKER_ICON);
        velocityContext.put(MapComponent.PARAMETER_DEFAULT_MARKER_ICON, defaultMarkerIcon != null ? defaultMarkerIcon: "");
    }
}
