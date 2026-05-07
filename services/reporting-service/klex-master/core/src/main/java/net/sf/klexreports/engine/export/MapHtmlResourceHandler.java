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
package net.sf.klexreports.engine.export;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class MapHtmlResourceHandler implements HtmlResourceHandler 
{
	/**
	 * 
	 */
	private HtmlResourceHandler parentHandler;
	private Map<String, byte[]> resourcesMap;

	/**
	 * 
	 */
	public MapHtmlResourceHandler(HtmlResourceHandler parentHandler, Map<String, byte[]> resourcesMap)
	{
		this.parentHandler = parentHandler;
		this.resourcesMap = resourcesMap;
	}

	/**
	 * 
	 */
	public MapHtmlResourceHandler(HtmlResourceHandler parentHandler)
	{
		this(parentHandler, new HashMap<>());
	}

	/**
	 * 
	 */
	public MapHtmlResourceHandler(Map<String, byte[]> resourcesMap)
	{
		this(null, resourcesMap);
	}

	/**
	 * 
	 */
	public MapHtmlResourceHandler()
	{
		this((HtmlResourceHandler)null);
	}

	@Override
	public String getResourcePath(String id)
	{
		if (parentHandler == null)
		{
			return id;
		}
		return parentHandler.getResourcePath(id);
	}

	@Override
	public void handleResource(String id, byte[] data)
	{
		if (parentHandler != null)
		{
			parentHandler.handleResource(id, data);
		}
		if (resourcesMap != null)
		{
			resourcesMap.put(id, data);
		}
	}

	/**
	 * 
	 */
	public Map<String, byte[]> getResourcesMap()
	{
		return resourcesMap;
	}
}
