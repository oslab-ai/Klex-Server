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
package net.sf.klexreports.interactivity.sort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;

import net.sf.klexreports.components.headertoolbar.HeaderToolbarElement;
import net.sf.klexreports.components.util.FieldFilter;
import net.sf.klexreports.components.util.FilterTypeBooleanOperatorsEnum;
import net.sf.klexreports.components.util.FilterTypeDateOperatorsEnum;
import net.sf.klexreports.components.util.FilterTypeNumericOperatorsEnum;
import net.sf.klexreports.components.util.FilterTypeTextOperatorsEnum;
import net.sf.klexreports.components.util.FilterTypesEnum;
import net.sf.klexreports.engine.DatasetFilter;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRSortField;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.engine.design.JRDesignDataset;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.type.NamedEnum;
import net.sf.klexreports.engine.type.SortFieldTypeEnum;
import net.sf.klexreports.engine.util.MessageProvider;
import net.sf.klexreports.engine.util.MessageUtil;
import net.sf.klexreports.interactivity.commands.CommandTarget;
import net.sf.klexreports.interactivity.sort.actions.FilterAction;
import net.sf.klexreports.interactivity.sort.actions.SortAction;
import net.sf.klexreports.interactivity.sort.actions.SortData;
import net.sf.klexreports.jackson.util.JacksonUtil;
import net.sf.klexreports.json.export.GenericElementJsonHandler;
import net.sf.klexreports.json.export.JsonExporterContext;
import net.sf.klexreports.repo.KlexDesignCache;
import net.sf.klexreports.velocity.util.VelocityUtil;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class SortElementJsonHandler implements GenericElementJsonHandler
{
	private static final Log log = LogFactory.getLog(SortElementJsonHandler.class);
	
	private static final String SORT_ELEMENT_HTML_TEMPLATE = "net/sf/klexreports/components/sort/resources/SortElementJsonTemplate.vm";
	private static final String PARAM_GENERATED_TEMPLATE = "net.sf.klexreports.sort";

	private static final String SORT_DATASET = "exporter_first_attempt";

	@Override
	public String getJsonFragment(JsonExporterContext context, JRGenericPrintElement element)
	{
		String htmlFragment = null;
		ReportContext reportContext = context.getExporterRef().getReportContext();
		if (reportContext != null)//FIXMEJIVE
		{
			String sortColumnName = (String) element.getParameterValue(SortElement.PARAMETER_SORT_COLUMN_NAME);
			String sortColumnType = (String) element.getParameterValue(SortElement.PARAMETER_SORT_COLUMN_TYPE);
			
			String sortDatasetName = element.getPropertiesMap().getProperty(SortElement.PROPERTY_DATASET_RUN);
			boolean templateAlreadyLoaded = false;
			
			FilterTypesEnum filterType = FilterTypesEnum.getByName(element.getPropertiesMap().getProperty(SortElement.PROPERTY_FILTER_TYPE));
			if (filterType == null)//FIXMEJIVE
			{
				return null;
			}
			
			String filterPattern = element.getPropertiesMap().getProperty(SortElement.PROPERTY_FILTER_PATTERN);
			Locale locale = (Locale) reportContext.getParameterValue(JRParameter.REPORT_LOCALE);
			KlexReportsContext jrContext = context.getKlexReportsContext();
			
			if (log.isDebugEnabled()) {
				log.debug("report locale: " + locale);
			}
			
			if (locale == null) {
				locale = Locale.getDefault();
			}
			
			VelocityContext contextMap = new VelocityContext();

			Boolean isClearCache = (Boolean)reportContext.getParameterValue(PARAMETER_CLEAR_CONTEXT_CACHE);

			if (reportContext.getParameterValue(PARAM_GENERATED_TEMPLATE) != null && !(isClearCache != null && isClearCache)) {
				templateAlreadyLoaded = true;
			} else {
				reportContext.setParameterValue(PARAM_GENERATED_TEMPLATE, true);
			}
			contextMap.put("templateAlreadyLoaded", templateAlreadyLoaded);

			if (!sortDatasetName.equals(context.getValue(SORT_DATASET))) 
			{
				context.setValue(SORT_DATASET, sortDatasetName);

				// operators
				contextMap.put("numericOperators", JacksonUtil.getInstance(jrContext).getJsonString(getTranslatedOperators(jrContext, FilterTypeNumericOperatorsEnum.class.getName(), FilterTypeNumericOperatorsEnum.values(), locale)));
				contextMap.put("dateOperators", JacksonUtil.getInstance(jrContext).getJsonString(getTranslatedOperators(jrContext, FilterTypeDateOperatorsEnum.class.getName(), FilterTypeDateOperatorsEnum.values(), locale)));
				contextMap.put("timeOperators", JacksonUtil.getInstance(jrContext).getJsonString(getTranslatedOperators(jrContext, FilterTypeDateOperatorsEnum.class.getName(), FilterTypeDateOperatorsEnum.values(), locale)));
				contextMap.put("textOperators", JacksonUtil.getInstance(jrContext).getJsonString(getTranslatedOperators(jrContext, FilterTypeTextOperatorsEnum.class.getName(), FilterTypeTextOperatorsEnum.values(), locale)));
				contextMap.put("booleanOperators", JacksonUtil.getInstance(jrContext).getJsonString(getTranslatedOperators(jrContext, FilterTypeBooleanOperatorsEnum.class.getName(), FilterTypeBooleanOperatorsEnum.values(), locale)));
				
				contextMap.put("exporterFirstAttempt", true);
			}


			String sortField = getCurrentSortField(context.getKlexReportsContext(), reportContext, element.getUUID().toString(), sortDatasetName, sortColumnName, sortColumnType);
			SortData sortData;
			if (sortField == null) 
			{
				sortData = new SortData(element.getUUID().toString(), sortColumnName, sortColumnType, SortElement.SORT_ORDER_ASC);
			}
			else 
			{
				String[] sortActionData = SortElementUtils.extractColumnInfo(sortField);
				boolean isAscending = SortElement.SORT_ORDER_ASC.equals(sortActionData[2]);
				String sortOrder = !isAscending ? SortElement.SORT_ORDER_NONE : SortElement.SORT_ORDER_DESC;
				sortData = new SortData(element.getUUID().toString(), sortColumnName, sortColumnType, sortOrder);
			}

			// existing filters
			String filterValueStart = "";
			String filterValueEnd = "";
			String filterTypeOperator = "";
			List<FieldFilter> fieldFilters = getExistingFiltersForField(context.getKlexReportsContext(), reportContext, element.getUUID().toString(), sortColumnName);
			
			if (fieldFilters.size() > 0) {
				FieldFilter ff = fieldFilters.get(0);
				if (ff.getFilterValueStart() != null) {
					filterValueStart = ff.getFilterValueStart();
				}
				if (ff.getFilterValueEnd() != null) {
					filterValueEnd = ff.getFilterValueEnd();
				}
				filterTypeOperator = ff.getFilterTypeOperator();
			}

			contextMap.put("id", element.hashCode());
			contextMap.put("uuid", element.getUUID().toString());
			contextMap.put("isFilterable", filterType != null);

			contextMap.put("datasetUuid", element.getUUID().toString());
			contextMap.put("actionData", getActionData(context, sortData));
			
			contextMap.put("isField", SortFieldTypeEnum.FIELD.equals(SortFieldTypeEnum.getByName(sortColumnType)));
			contextMap.put("fieldName", sortColumnName);
			contextMap.put("fieldValueStart", filterValueStart);
			contextMap.put("fieldValueEnd", filterValueEnd);
			contextMap.put("filterType", filterType.getName());
			contextMap.put("filterTypeOperator", filterTypeOperator);
			contextMap.put("filterPattern", filterPattern != null ? filterPattern : "");
//			velocityContext.put("localeCode",);
//			velocityContext.put("timeZoneId",);

			htmlFragment = VelocityUtil.processTemplate(SortElementJsonHandler.SORT_ELEMENT_HTML_TEMPLATE, contextMap);
		}
		
		return htmlFragment;
	}
	
	private String getCurrentSortField(KlexReportsContext klexReportsContext, ReportContext reportContext, String uuid, String sortDatasetName, String sortColumnName, String sortColumnType) 
	{
		KlexDesignCache cache = KlexDesignCache.getInstance(klexReportsContext, reportContext);
		SortAction action = new SortAction();
		action.init(klexReportsContext, reportContext);
		CommandTarget target = action.getCommandTarget(UUID.fromString(uuid), false);
		if (target != null)
		{
			KlexDesign klexDesign = cache.getKlexDesign(target.getUri(), false);
			JRDesignDataset dataset = (JRDesignDataset)klexDesign.getMainDataset();
			
			List<JRSortField> existingFields =  dataset.getSortFieldsList();
			String sortField = null;
	
			if (existingFields != null && existingFields.size() > 0) {
				for (JRSortField field: existingFields) {
					if (field.getName().equals(sortColumnName) && field.getType().getName().equals(sortColumnType)) {
						sortField = sortColumnName + SortElement.SORT_COLUMN_TOKEN_SEPARATOR + sortColumnType + SortElement.SORT_COLUMN_TOKEN_SEPARATOR;
						switch (field.getOrder()) {
							case ASCENDING:
								sortField += SortElement.SORT_ORDER_ASC;
								break;
							case DESCENDING:
								sortField += SortElement.SORT_ORDER_DESC;
								break;
						}
						break;
					}
				}
			}
		
			return sortField;
		}
		
		return null;
	}
	
	@Override
	public boolean toExport(JRGenericPrintElement element) {
		return true;
	}
	
	private List<LinkedHashMap<String, String>> getTranslatedOperators(
			KlexReportsContext klexReportsContext,
			String bundleName,
			NamedEnum[] operators,
			Locale locale
	) //FIXMEJIVE make utility method for translating enums
	{
		List<LinkedHashMap<String, String>> result = new ArrayList<>();
		MessageProvider messageProvider = MessageUtil.getInstance(klexReportsContext).getMessageProvider(bundleName);
		LinkedHashMap<String, String> keys;

		for (NamedEnum operator: operators)
		{
			keys = new LinkedHashMap<>();
			String key = bundleName + "." + ((Enum<?>)operator).name();
			keys.put("key", ((Enum<?>)operator).name());
			keys.put("val", messageProvider.getMessage(key, null, locale));
			result.add(keys);
		}

		return result;
	}
	
	private String getActionData(JsonExporterContext context, SortData sortData) {
		return "{\"actionName\":\"sortica\",\"sortData\":" + JacksonUtil.getInstance(context.getKlexReportsContext()).getJsonString(sortData)+ "}";
	}
	
	private List<FieldFilter> getExistingFiltersForField(
			KlexReportsContext klexReportsContext, 
			ReportContext reportContext, 
			String uuid, 
			String filterFieldName
			) 
		{
			KlexDesignCache cache = KlexDesignCache.getInstance(klexReportsContext, reportContext);
			FilterAction action = new FilterAction();
			action.init(klexReportsContext, reportContext);
			CommandTarget target = action.getCommandTarget(UUID.fromString(uuid), false);
			List<FieldFilter> result = new ArrayList<>();
			if (target != null)
			{
				KlexDesign klexDesign = cache.getKlexDesign(target.getUri(), false);
				JRDesignDataset dataset = (JRDesignDataset)klexDesign.getMainDataset();
				
				// get existing filter as JSON string
				String serializedFilters = "[]";
				JRPropertiesMap propertiesMap = dataset.getPropertiesMap();
				if (propertiesMap.getProperty(HeaderToolbarElement.DATASET_FILTER_PROPERTY) != null) {
					serializedFilters = propertiesMap.getProperty(HeaderToolbarElement.DATASET_FILTER_PROPERTY);
				}
				
				List<? extends DatasetFilter> existingFilters = JacksonUtil.getInstance(klexReportsContext).loadList(serializedFilters, FieldFilter.class);
				if (existingFilters.size() > 0) {
					for (DatasetFilter filter: existingFilters) {
						if (((FieldFilter)filter).getField().equals(filterFieldName)) {
							result.add((FieldFilter)filter);
							break;
						}
					}
				}
			}
			
			return result;
		}
}
