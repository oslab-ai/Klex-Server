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
package net.sf.klexreports.repo;

import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.ReportContext;



/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class KlexDesignReportResourceCache
{
	/**
	 * 
	 */
	private static final String PARAMETER_KLEX_DESIGN_REPORT_RESOURCE_CACHE = "net.sf.klexreports.parameter.klexdesign.report.resource.cache";

	/**
	 * 
	 */
	private Map<String, KlexDesignReportResource> cachedResourcesMap = new HashMap<>();

	/**
	 * 
	 */
	public static KlexDesignReportResourceCache getInstance(ReportContext reportContext)
	{
		KlexDesignReportResourceCache cache = (KlexDesignReportResourceCache)reportContext.getParameterValue(PARAMETER_KLEX_DESIGN_REPORT_RESOURCE_CACHE);
		
		if (cache == null)
		{
			cache = new KlexDesignReportResourceCache();
			reportContext.setParameterValue(PARAMETER_KLEX_DESIGN_REPORT_RESOURCE_CACHE, cache);
		}
		
		return cache;
	}
	
	/**
	 * 
	 */
	public KlexDesignReportResource getResource(String uri)
	{
		return cachedResourcesMap.get(uri);
	}

	/**
	 * 
	 */
	public void setResource(String uri, KlexDesignReportResource resource)
	{
		cachedResourcesMap.put(uri, resource);
	}
}
