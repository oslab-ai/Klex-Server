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

import net.sf.klexreports.data.excel.ExcelFormatEnum;
import net.sf.klexreports.data.xls.AbstractXlsDataAdapterService;
import net.sf.klexreports.data.xls.XlsDataAdapter;
import net.sf.klexreports.data.xlsx.XlsxDataAdapter;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.ParameterContributorContext;
import net.sf.klexreports.engine.data.AbstractXlsDataSource;
import net.sf.klexreports.engine.util.JRClassLoader;
import net.sf.klexreports.poi.query.ExcelQueryExecuter;
import net.sf.klexreports.poi.query.ExcelQueryExecuterFactory;

/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class XlsxDataAdapterService extends AbstractXlsDataAdapterService 
{
	
	/**
	 * 
	 */
	public XlsxDataAdapterService(ParameterContributorContext paramContribContext, XlsxDataAdapter xlsxDataAdapter)
	{
		super(paramContribContext, xlsxDataAdapter);
	}
	
	public XlsxDataAdapter getXlsxDataAdapter()
	{
		return (XlsxDataAdapter)getDataAdapter();
	}
	
	@Override
	public void contributeParameters(Map<String, Object> parameters) throws JRException
	{
		super.contributeParameters(parameters);

		XlsDataAdapter xlsDataAdapter = getXlsDataAdapter();
		if (xlsDataAdapter != null)
		{
			if (xlsDataAdapter.isQueryExecuterMode())
			{	
				parameters.put(ExcelQueryExecuterFactory.XLS_FORMAT, ExcelFormatEnum.XLSX);//add this just for the sake of ExcelQueryExecuter, which is called when queryMode=true
			}
		}
	}

	@Override
	protected AbstractXlsDataSource getXlsDataSource() throws JRException
	{
		AbstractXlsDataSource dataSource = null;

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
						new Object[]{dataStream, false}
						);
			}
			catch (ClassNotFoundException e)
			{
				dataSource =
					ExcelQueryExecuter.createDataSource(
						ExcelQueryExecuter.EXCEL_DATA_SOURCE_CLASS,
						new Class<?>[]{InputStream.class, boolean.class, ExcelFormatEnum.class},
						new Object[]{dataStream, false, ExcelFormatEnum.XLSX}
						);
			}
		}
		else
		{
			dataSource =
				ExcelQueryExecuter.createDataSource(
					dataSourceFactoryClassName,
					dataStream,
					false
					);
		}

		return dataSource;
	}
	
}
