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
package net.sf.klexreports.engine.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ReportWriterConfiguration
{
	
	/**
	 *
	 */
	private KlexReportsContext klexReportsContext;
	
	private List<Pattern> excludePropertiesPattern;
	private boolean excludeUuids;

	/**
	 *
	 */
	public ReportWriterConfiguration(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
		
		initExcludeProperties();
	}


	private void initExcludeProperties()
	{
		KlexReportsContext context = klexReportsContext == null 
				? DefaultKlexReportsContext.getInstance() : klexReportsContext;
		List<PropertySuffix> excludeProperties = JRPropertiesUtil.getInstance(context).getProperties(
				JRXmlWriter.PREFIX_EXCLUDE_PROPERTIES);

		excludePropertiesPattern = new ArrayList<>(excludeProperties.size());
		for (PropertySuffix propertySuffix : excludeProperties)
		{
			String regex = propertySuffix.getValue();
			Pattern pattern = Pattern.compile(regex);
			excludePropertiesPattern.add(pattern);
		}

		excludeUuids = JRPropertiesUtil.getInstance(context).getBooleanProperty(JRXmlWriter.PROPERTY_EXCLUDE_UUIDS);
	}


	/**
	 *
	 */
	public KlexReportsContext getKlexReportsContext()
	{
		return klexReportsContext;
	}


	/**
	 *
	 */
	public boolean isExcludeUuids()
	{
		return excludeUuids;
	}


	/**
	 *
	 */
	public boolean isPropertyToWrite(String propertyName)
	{
		boolean toWrite = true;
		for (Pattern pattern : excludePropertiesPattern)
		{
			if (pattern.matcher(propertyName).matches())
			{
				// excluding
				toWrite = false;
				break;
			}
		}
		return toWrite;
	}
	
}
