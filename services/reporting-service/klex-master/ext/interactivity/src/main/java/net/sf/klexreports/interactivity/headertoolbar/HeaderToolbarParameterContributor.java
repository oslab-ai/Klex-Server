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
package net.sf.klexreports.interactivity.headertoolbar;

import java.util.List;
import java.util.Map;

import net.sf.klexreports.components.headertoolbar.HeaderToolbarElement;
import net.sf.klexreports.components.util.FieldFilter;
import net.sf.klexreports.engine.CompositeDatasetFilter;
import net.sf.klexreports.engine.DatasetFilter;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.ParameterContributor;
import net.sf.klexreports.engine.ParameterContributorContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.util.JsonLoader;

/**
 * 
 * 
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class HeaderToolbarParameterContributor implements ParameterContributor
{
	private final ParameterContributorContext context;

	public HeaderToolbarParameterContributor (ParameterContributorContext context)
	{
		this.context = context;
	}
	
	@Override
	public void contributeParameters(Map<String, Object> parameterValues) throws JRException
	{
		ReportContext reportContext = (ReportContext) parameterValues.get(JRParameter.REPORT_CONTEXT);
		if (reportContext != null)
		{
			String serializedFilters = JRPropertiesUtil.getOwnProperty(context.getDataset(), HeaderToolbarElement.DATASET_FILTER_PROPERTY);
			
			if (serializedFilters != null) 
			{
				List<? extends DatasetFilter> existingFilters = getFilters(serializedFilters);
				parameterValues.put(JRParameter.FILTER, new CompositeDatasetFilter(existingFilters));
			}
		}
	}
	
	protected List<? extends DatasetFilter> getFilters(String serializedFilters)
	{
		if (serializedFilters != null) 
		{
			return JsonLoader.getInstance(context.getKlexReportsContext()).loadList(serializedFilters, FieldFilter.class);
		}
		return null;
	}
	
	@Override
	public void dispose() {
	}

}
