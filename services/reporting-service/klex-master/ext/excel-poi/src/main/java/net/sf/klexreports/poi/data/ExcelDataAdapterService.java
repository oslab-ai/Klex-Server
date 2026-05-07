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
package net.sf.klexreports.poi.data;

import java.io.InputStream;
import java.util.Map;

import net.sf.klexreports.data.excel.ExcelDataAdapter;
import net.sf.klexreports.data.excel.ExcelFormatEnum;
import net.sf.klexreports.data.xls.AbstractXlsDataAdapterService;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.ParameterContributorContext;
import net.sf.klexreports.engine.data.AbstractXlsDataSource;
import net.sf.klexreports.engine.util.JRClassLoader;
import net.sf.klexreports.engine.util.Pair;
import net.sf.klexreports.poi.query.ExcelQueryExecuter;
import net.sf.klexreports.poi.query.ExcelQueryExecuterFactory;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ExcelDataAdapterService extends AbstractXlsDataAdapterService 
{
	
	/**
	 * 
	 */
	public ExcelDataAdapterService(ParameterContributorContext paramContribContext, ExcelDataAdapter excelDataAdapter)
	{
		super(paramContribContext, excelDataAdapter);
	}

	public ExcelDataAdapter getExcelDataAdapter()
	{
		return (ExcelDataAdapter)getDataAdapter();
	}

	@Override
	public void contributeParameters(Map<String, Object> parameters) throws JRException
	{
		super.contributeParameters(parameters);

		ExcelDataAdapter xlsDataAdapter = getExcelDataAdapter();
		if (xlsDataAdapter != null)
		{
			ExcelFormatEnum format = xlsDataAdapter.getFormat();

			if (xlsDataAdapter.isQueryExecuterMode())
			{	
				if (format != null) 
				{
					parameters.put(ExcelQueryExecuterFactory.XLS_FORMAT, format);
				}
			}
		}
	}

	@Override
	protected AbstractXlsDataSource getXlsDataSource() throws JRException
	{
		AbstractXlsDataSource dataSource = null;

		ExcelDataAdapter excelDataAdapter = getExcelDataAdapter();
		
		InputStream inputStream = dataStream;
		ExcelFormatEnum format = excelDataAdapter.getFormat();
		if (format == null || format == ExcelFormatEnum.AUTODETECT)
		{
			Pair<InputStream, ExcelFormatEnum> sniffResult = ExcelQueryExecuter.sniffExcelFormat(inputStream);
			inputStream = sniffResult.first();
			format = sniffResult.second();
		}
		
		switch (format)
		{
			case XLS :
			{
				dataSource =
					ExcelQueryExecuter.createDataSource(
						ExcelQueryExecuter.EXCEL_DATA_SOURCE_CLASS,
						new Class<?>[]{InputStream.class, boolean.class, ExcelFormatEnum.class},
						new Object[]{inputStream, false, format}
						);
				break;
			}
			case XLSX :
			{
				String dataSourceFactoryClassName = 
					JRPropertiesUtil.getInstance(getKlexReportsContext()).getProperty(
						ExcelQueryExecuter.PROPERTY_XLSX_DATA_SOURCE_FACTORY, 
						getParameterContributorContext().getDataset()
						);
				if (dataSourceFactoryClassName == null)
				{
					try
					{
						JRClassLoader.loadClassForName(ExcelQueryExecuter.FASTEXCEL_DATA_SOURCE_CLASS);
						dataSource =
							ExcelQueryExecuter.createDataSource(
								ExcelQueryExecuter.FASTEXCEL_DATA_SOURCE_CLASS,
								new Class<?>[]{InputStream.class, boolean.class},
								new Object[]{inputStream, false}
								);
					}
					catch (ClassNotFoundException e)
					{
						dataSource =
							ExcelQueryExecuter.createDataSource(
								ExcelQueryExecuter.EXCEL_DATA_SOURCE_CLASS,
								new Class<?>[]{InputStream.class, boolean.class, ExcelFormatEnum.class},
								new Object[]{inputStream, false, format}
								);
					}
				}
				else
				{
					dataSource =
						ExcelQueryExecuter.createDataSource(
							dataSourceFactoryClassName,
							inputStream,
							false
							);
				}
				
				break;
			}
			case AUTODETECT :
			default:
			{
				// should never get here
			}
		}
		
		return dataSource;
	}

}
