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
package net.sf.klexreports.web.util;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public interface WebConstants
{
	public static final String APPLICATION_CONTEXT_PATH = "net.sf.klexreports.web.app.context.path";

	public static final String REQUEST_PARAMETER_APPLICATION_DOMAIN = "jr_app_domain";
	public static final String REQUEST_PARAMETER_REPORT_URI = "jr_report_uri";
	public static final String REQUEST_PARAMETER_ASYNC_REPORT = "jr_async";
	public static final String REQUEST_PARAMETER_PAGE = "jr_page";
	public static final String REQUEST_PARAMETER_PAGE_TIMESTAMP = "jr_page_timestamp";
	public static final String REQUEST_PARAMETER_PAGE_UPDATE = "jr_page_update";
	public static final String REQUEST_PARAMETER_RUN_REPORT = "jr_run";
	
	/**
	 * 
	 */
	public static final String PROPERTIES_WEB_RESOURCE_PATTERN_PREFIX = "net.sf.klexreports.web.resource.pattern.";

	/**
	 * Boolean property to control the setting of the response header Access-Control-Allow-Origin to *
	 */
	public static final String PROPERTY_ACCESS_CONTROL_ALLOW_ORIGIN = "net.sf.klexreports.web.resource.cors.header.allow.origin.all";

	@Property(
			category = PropertyConstants.CATEGORY_WEB_UTIL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_4_6_0
			)
	public static final String PROPERTY_REQUEST_PARAMETER_RESOURCE_URI = JRPropertiesUtil.PROPERTY_PREFIX + "web.request.parameter.resource.uri";
	@Property(
			category = PropertyConstants.CATEGORY_WEB_UTIL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_4_6_0
			)
	public static final String PROPERTY_REQUEST_PARAMETER_RESOURCE_LOCALE = JRPropertiesUtil.PROPERTY_PREFIX + "web.request.parameter.resource.locale";
	@Property(
			category = PropertyConstants.CATEGORY_WEB_UTIL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_4_6_0
			)
	public static final String PROPERTY_REQUEST_PARAMETER_RESOURCE_BUNDLE = JRPropertiesUtil.PROPERTY_PREFIX + "web.request.parameter.resource.bundle";
	@Property(
			category = PropertyConstants.CATEGORY_WEB_UTIL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_4_6_0
			)
	public static final String PROPERTY_REQUEST_PARAMETER_DYNAMIC_RESOURCE = JRPropertiesUtil.PROPERTY_PREFIX + "web.request.parameter.dynamic.resource";
	@Property(
			category = PropertyConstants.CATEGORY_WEB_UTIL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_4_6_0
			)
	public static final String PROPERTY_REPORT_EXECUTION_PATH = JRPropertiesUtil.PROPERTY_PREFIX + "web.report.execution.path";
	@Property(
			category = PropertyConstants.CATEGORY_WEB_UTIL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_4_6_0
			)
	public static final String PROPERTY_REPORT_RESOURCES_PATH = JRPropertiesUtil.PROPERTY_PREFIX + "web.report.resources.path";
	@Property(
			category = PropertyConstants.CATEGORY_WEB_UTIL,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_5_5_0,
			valueType = Boolean.class
			)
	public static final String PROPERTY_EMBED_COMPONENT_METADATA = JRPropertiesUtil.PROPERTY_PREFIX + "web.embed.component.metadata.in.html.output";

}
