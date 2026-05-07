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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Workbook;

import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.AbstractXlsQueryExecuter;
import net.sf.klexreports.engine.query.AbstractXlsQueryExecuterFactory;
import net.sf.klexreports.engine.query.QueryExecutionContext;
import net.sf.klexreports.engine.query.SimpleQueryExecutionContext;
import net.sf.klexreports.poi.data.JRXlsxDataSource;

/**
 * XLS query executer implementation.
 * 
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 * @deprecated Replaced by {@link ExcelQueryExecuter}.
 */
public class JRXlsxQueryExecuter extends AbstractXlsQueryExecuter
{
	
	private static final Log log = LogFactory.getLog(JRXlsxQueryExecuter.class);
	
	private JRXlsxDataSource datasource;
	
	/**
	 * 
	 */
	protected JRXlsxQueryExecuter(
		KlexReportsContext klexReportsContext, 
		JRDataset dataset, 
		Map<String,? extends JRValueParameter> parametersMap
		)
	{
		this(SimpleQueryExecutionContext.of(klexReportsContext), 
				dataset, parametersMap);
	}
	
	protected JRXlsxQueryExecuter(
		QueryExecutionContext context, 
		JRDataset dataset, 
		Map<String,? extends JRValueParameter> parametersMap
		) 
	{
		super(context, dataset, parametersMap);
	}

	@Override
	public JRDataSource createDatasource() throws JRException {
		try {
			Workbook workbook = (Workbook) getParameterValue(JRXlsxQueryExecuterFactory.XLSX_WORKBOOK);
			if (workbook == null)
			{
				workbook = (Workbook) getParameterValue(AbstractXlsQueryExecuterFactory.XLS_WORKBOOK, true);
			}
			if (workbook != null) {
				datasource = new JRXlsxDataSource(workbook);
			} else {
				InputStream xlsxInputStream = (InputStream) getParameterValue(JRXlsxQueryExecuterFactory.XLSX_INPUT_STREAM);
				if (xlsxInputStream == null)
				{
					xlsxInputStream = (InputStream) getParameterValue(AbstractXlsQueryExecuterFactory.XLS_INPUT_STREAM, true);
				}
				if (xlsxInputStream != null) {
					datasource = new JRXlsxDataSource(xlsxInputStream);
				} else {
					File xlsxFile = (File) getParameterValue(JRXlsxQueryExecuterFactory.XLSX_FILE);
					if (xlsxFile == null)
					{
						xlsxFile = (File) getParameterValue(AbstractXlsQueryExecuterFactory.XLS_FILE, true);
					}
					if (xlsxFile != null) {
						datasource = new JRXlsxDataSource(xlsxFile);
					} else {
						String xlsxSource = getStringParameterOrProperty(JRXlsxQueryExecuterFactory.XLSX_SOURCE);
						if (xlsxSource == null)
						{
							xlsxSource = getStringParameterOrProperty(AbstractXlsQueryExecuterFactory.XLS_SOURCE);
						}
						if (xlsxSource != null) {
							datasource = new JRXlsxDataSource(getRepositoryContext(), xlsxSource);//TODO
						} else {
							if (log.isWarnEnabled()){
								log.warn("No XLS source was provided.");
							}
						}
					}
				}
			}
		} catch (IOException e) {
			throw new JRException(e);
		}
		
		if (datasource != null) {
			initDatasource(datasource);
		}
		
		return datasource;
	}
	
}
