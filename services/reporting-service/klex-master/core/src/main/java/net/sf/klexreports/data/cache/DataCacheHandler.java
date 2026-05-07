/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.data.cache;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * Report data cache handler.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface DataCacheHandler
{
	
	String PARAMETER_DATA_CACHE_HANDLER = "net.sf.klexreports.data.cache.handler";
	
	@Property(
			category = PropertyConstants.CATEGORY_DATA_CACHE,
			defaultValue = PropertyConstants.BOOLEAN_TRUE,
			scopes = {PropertyScope.CONTEXT, PropertyScope.DATASET},
			sinceVersion = PropertyConstants.VERSION_4_6_0,
			valueType = Boolean.class
			)
	String PROPERTY_DATA_RECORDABLE = JRPropertiesUtil.PROPERTY_PREFIX + "data.cache.recordable";
	
	@Property(
			category = PropertyConstants.CATEGORY_DATA_CACHE,
			defaultValue = PropertyConstants.BOOLEAN_TRUE,
			scopes = {PropertyScope.CONTEXT, PropertyScope.DATASET},
			sinceVersion = PropertyConstants.VERSION_4_6_0,
			valueType = Boolean.class
			)
	String PROPERTY_DATA_PERSISTABLE = JRPropertiesUtil.PROPERTY_PREFIX + "data.cache.persistable";
	
	@Property(
			category = PropertyConstants.CATEGORY_DATA_CACHE,
			scopes = {PropertyScope.PARAMETER, PropertyScope.SUBDATASET_RUN, PropertyScope.SUBREPORT, PropertyScope.PART},
			sinceVersion = PropertyConstants.VERSION_4_6_0,
			valueType = Boolean.class
			)
	String PROPERTY_INCLUDED = JRPropertiesUtil.PROPERTY_PREFIX + "data.cache.included";

	boolean isRecordingEnabled();
	
	DataRecorder createDataRecorder();
	
	boolean isSnapshotPopulated();
	
	DataSnapshot getDataSnapshot();
	
}
