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
package net.sf.klexreports.governors;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRDefaultScriptlet;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRScriptletException;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class MaxPagesGovernor extends JRDefaultScriptlet
{

	/**
	 *
	 */
	@Property(
			category = PropertyConstants.CATEGORY_GOVERNOR,
			valueType = Boolean.class,
			defaultValue = PropertyConstants.BOOLEAN_FALSE,
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT},
			sinceVersion = PropertyConstants.VERSION_3_1_4
			)
	public static final String PROPERTY_MAX_PAGES_ENABLED = JRPropertiesUtil.PROPERTY_PREFIX + "governor.max.pages.enabled";
	
	@Property(
			category = PropertyConstants.CATEGORY_GOVERNOR,
			valueType = Integer.class,
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT},
			sinceVersion = PropertyConstants.VERSION_3_1_4
			)
	public static final String PROPERTY_MAX_PAGES = JRPropertiesUtil.PROPERTY_PREFIX + "governor.max.pages";

	/**
	 *
	 */
	private int pages;
	private int maxPages;

	
	/**
	 *
	 */
	public MaxPagesGovernor(int maxPages)
	{
		this.maxPages = maxPages;
	}


	@Override
	public void afterPageInit() throws JRScriptletException
	{
		// cannot use PAGE_NUMBER variable because of timing issues and because of isResetPageNumber flag
		pages++;
		if (maxPages < pages)
		{
			throw 
				new MaxPagesGovernorException(
					((KlexReport)getParameterValue(JRParameter.KLEX_REPORT, false)).getName(),
					maxPages
					);
		}
	}


}
