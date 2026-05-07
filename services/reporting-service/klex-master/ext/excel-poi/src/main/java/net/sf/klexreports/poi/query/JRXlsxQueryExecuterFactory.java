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

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.AbstractXlsQueryExecuterFactory;
import net.sf.klexreports.engine.query.JRQueryExecuter;
import net.sf.klexreports.engine.query.QueryExecutionContext;
import net.sf.klexreports.engine.query.SimpleQueryExecutionContext;

/**
 * Query executer factory for XLSX file type.
 * <p/>
 * The factory creates {@link net.sf.klexreports.poi.query.JRXlsxQueryExecuter JRXlsxQueryExecuter}
 * query executers.
 * 
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 * @deprecated Replaced by {@link ExcelQueryExecuterFactory}.
 */
public class JRXlsxQueryExecuterFactory extends AbstractXlsQueryExecuterFactory
{
	private final static Object[] XLSX_BUILTIN_PARAMETERS = {
			XLSX_WORKBOOK, "org.apache.poi.ss.usermodel.Workbook",
			XLS_WORKBOOK, "org.apache.poi.ss.usermodel.Workbook",
			XLSX_INPUT_STREAM, "java.io.InputStream",
			XLS_INPUT_STREAM, "java.io.InputStream",
			XLSX_FILE, "java.io.File",
			XLS_FILE, "java.io.File",
			XLSX_SOURCE, "java.lang.String",
			XLS_SOURCE, "java.lang.String",
			XLSX_COLUMN_NAMES, "java.lang.String",
			XLS_COLUMN_NAMES, "java.lang.String",
			XLSX_COLUMN_INDEXES, "java.lang.String",
			XLS_COLUMN_INDEXES, "java.lang.String",
			XLSX_COLUMN_NAMES_ARRAY, "java.lang.String[]",
			XLS_COLUMN_NAMES_ARRAY, "java.lang.String[]",
			XLSX_COLUMN_INDEXES_ARRAY, "java.lang.Integer[]",
			XLS_COLUMN_INDEXES_ARRAY, "java.lang.Integer[]",
			XLSX_DATE_FORMAT, "java.text.DateFormat",
			XLS_DATE_FORMAT, "java.text.DateFormat",
			XLSX_DATE_PATTERN, "java.lang.String",
			XLS_DATE_PATTERN, "java.lang.String",
			XLSX_NUMBER_FORMAT, "java.text.NumberFormat",
			XLS_NUMBER_FORMAT, "java.text.NumberFormat",
			XLSX_NUMBER_PATTERN, "java.lang.String",
			XLS_NUMBER_PATTERN, "java.lang.String",
			XLSX_USE_FIRST_ROW_AS_HEADER, "java.lang.Boolean",
			XLS_USE_FIRST_ROW_AS_HEADER, "java.lang.Boolean",
			XLSX_LOCALE, "java.util.Locale",
			XLS_LOCALE, "java.util.Locale",
			XLSX_LOCALE_CODE, "java.lang.String",
			XLS_LOCALE_CODE, "java.lang.String",
			XLSX_TIMEZONE, "java.util.TimeZone",
			XLS_TIMEZONE, "java.util.TimeZone",
			XLSX_TIMEZONE_ID, "java.lang.String",
			XLS_TIMEZONE_ID, "java.lang.String",
			XLS_SHEET_SELECTION, "java.lang.String"
			};
	
	@Override
	public Object[] getBuiltinParameters() {
		return XLSX_BUILTIN_PARAMETERS;
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
		return new JRXlsxQueryExecuter(context, dataset, parameters);
	}

	@Override
	public boolean supportsQueryParameterType(String className) {
		return true;
	}
}
