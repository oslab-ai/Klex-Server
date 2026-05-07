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
package net.sf.klexreports.poi.query;

import java.util.Map;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.AbstractXlsQueryExecuterFactory;
import net.sf.klexreports.engine.query.JRQueryExecuter;
import net.sf.klexreports.engine.query.QueryExecutionContext;
import net.sf.klexreports.engine.query.SimpleQueryExecutionContext;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * Query executer factory for Excel file type.
 * <p/>
 * The factory creates {@link net.sf.klexreports.poi.query.ExcelQueryExecuter ExcelQueryExecuter}
 * query executers.
 * 
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class ExcelQueryExecuterFactory extends AbstractXlsQueryExecuterFactory 
{
	/**
	 * Built-in parameter/property holding the value of the Excel format to be used when parsing the Excel data.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_DATA_SOURCE,
			scopes = {PropertyScope.CONTEXT, PropertyScope.DATASET},
			scopeQualifications = {AbstractXlsQueryExecuterFactory.QUERY_EXECUTER_NAME},
			sinceVersion = PropertyConstants.VERSION_5_5_2
			)
	public static final String XLS_FORMAT = JRPropertiesUtil.PROPERTY_PREFIX + "xls.format";

	private final static Object[] XLS_BUILTIN_PARAMETERS = {
			XLS_WORKBOOK, "org.apache.poi.ss.usermodel.Workbook",
			XLS_INPUT_STREAM, "java.io.InputStream",
			XLS_FILE, "java.io.File",
			XLS_SOURCE, "java.lang.String",
			XLS_COLUMN_NAMES, "java.lang.String",
			XLS_COLUMN_INDEXES, "java.lang.String",
			XLS_COLUMN_NAMES_ARRAY, "java.lang.String[]",
			XLS_COLUMN_INDEXES_ARRAY, "java.lang.Integer[]",
			XLS_DATE_FORMAT, "java.text.DateFormat",
			XLS_DATE_PATTERN, "java.lang.String",
			XLS_NUMBER_FORMAT, "java.text.NumberFormat",
			XLS_NUMBER_PATTERN, "java.lang.String",
			XLS_USE_FIRST_ROW_AS_HEADER, "java.lang.Boolean",
			XLS_LOCALE, "java.util.Locale",
			XLS_LOCALE_CODE, "java.lang.String",
			XLS_TIMEZONE, "java.util.TimeZone",
			XLS_TIMEZONE_ID, "java.lang.String",
			XLS_SHEET_SELECTION, "java.lang.String",
			XLS_FORMAT, "net.sf.klexreports.data.excel.ExcelFormatEnum"
			};
	
	@Override
	public Object[] getBuiltinParameters() {
		return XLS_BUILTIN_PARAMETERS;
	}

	@Override
	public JRQueryExecuter createQueryExecuter(
		KlexReportsContext klexReportsContext,
		JRDataset dataset, 
		Map<String,? extends JRValueParameter> parameters
		) throws JRException 
	{
		return createQueryExecuter(SimpleQueryExecutionContext.of(klexReportsContext), 
				dataset, parameters);
	}

	@Override
	public JRQueryExecuter createQueryExecuter(
		QueryExecutionContext context,
		JRDataset dataset, 
		Map<String,? extends JRValueParameter> parameters
		) throws JRException 
	{
		return new ExcelQueryExecuter(context, dataset, parameters);
	}

	@Override
	public boolean supportsQueryParameterType(String className) {
		return true;
	}
}
