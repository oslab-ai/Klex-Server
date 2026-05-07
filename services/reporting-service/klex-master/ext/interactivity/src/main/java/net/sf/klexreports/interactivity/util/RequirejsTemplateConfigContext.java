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
package net.sf.klexreports.interactivity.util;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.web.util.ResourcePathUtil;
import net.sf.klexreports.web.util.WebRequestContext;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class RequirejsTemplateConfigContext
{

	private static final Log log = LogFactory.getLog(RequirejsTemplateConfigContext.class);
	
	private String contextPath;
	private ResourcePathUtil resourcePathUtil;
	private Map<String, String> paths;
	private Map<String, String> resourcePaths;

	public RequirejsTemplateConfigContext(WebRequestContext context, Map<String, String> paths, Map<String, String> resourcePaths)
	{
		contextPath = context.getRequestContextPath();
		resourcePathUtil = ResourcePathUtil.getInstance(context.getKlexReportsContext());
		this.paths = paths;
		this.resourcePaths = resourcePaths;
	}
	
	public String getPath(String key)
	{
		String path = paths.get(key);
		
		if (path == null)
		{
			String resource = resourcePaths.get(key);
			if (resource != null)
			{
				path = contextPath + resourcePathUtil.getResourcesBasePath() + resource;
			}
		}
		
		if (log.isDebugEnabled())
		{
			log.debug("path for " + key + " is " + path);
		}
		
		//FIXME exception if not found?
		return path;
	}
}