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
package net.sf.klexreports.j2ee.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.MessageUtil;
import net.sf.klexreports.velocity.util.VelocityUtil;
import net.sf.klexreports.web.util.ResourcePathUtil;
import net.sf.klexreports.web.util.SimpleWebResource;
import net.sf.klexreports.web.util.WebResource;


/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JiveWebResourceHandler extends AbstractWebResourceHandler 
{
	
	private String bundleName;
	private Map<String, String> keyToFileMappings;
	
	public JiveWebResourceHandler(String bundleName) 
	{
		this.bundleName = bundleName;
		this.keyToFileMappings = new HashMap<>();
	}

	@Override
	public WebResource getResource(KlexReportsContext klexReportsContext, HttpServletRequest request, String resourceKey) 
	{
		SimpleWebResource resource = null;
		if (resourceKey != null && keyToFileMappings.containsKey(resourceKey)) 
		{
			byte[] bytes = null;

			try 
			{
				Locale locale = LocaleResolverUtil.instance(klexReportsContext).getLocale(request);
				Map<String, Object> contextMap = new HashMap<>();
				contextMap.put("path", request.getContextPath() + ResourcePathUtil.getInstance(klexReportsContext).getResourcesBasePath());
				contextMap.put("msgProvider", MessageUtil.getInstance(klexReportsContext).getLocalizedMessageProvider(bundleName, locale)); 
				String resourceString = VelocityUtil.processTemplate(keyToFileMappings.get(resourceKey), contextMap);
				if (resourceString != null) 
				{
					bytes = resourceString.getBytes("UTF-8");
				}
			}
			catch (IOException e) 
			{
				throw new JRRuntimeException(e);
			}
			
			resource = new SimpleWebResource();
			resource.setData(bytes);

			if (resourceKey != null && resourceKey.lastIndexOf(".") != -1) 
			{
				resource.setType(resourceKey.substring(resourceKey.lastIndexOf(".") + 1));
			}
		}
		return resource;
	}
	
	public void addMapping(String key, String fileClasspath) 
	{
		this.keyToFileMappings.put(key, fileClasspath);
	}
	
}
