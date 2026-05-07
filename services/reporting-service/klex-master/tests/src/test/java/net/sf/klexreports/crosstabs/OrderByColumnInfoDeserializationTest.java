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
package net.sf.klexreports.crosstabs;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import net.sf.klexreports.crosstabs.fill.calculation.OrderByColumnInfo;
import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.type.SortOrderEnum;
import net.sf.klexreports.interactivity.crosstabs.OrderByColumnInfoImpl;
import net.sf.klexreports.jackson.util.JacksonUtil;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class OrderByColumnInfoDeserializationTest
{
	
	private JacksonUtil jacksonUtil;

	@BeforeTest
	public void setup()
	{
		jacksonUtil = JacksonUtil.getInstance(DefaultKlexReportsContext.getInstance());
	}
	
	@Test(dataProvider = "sortOrders")
	public void sortOrderTest(String jsonText, SortOrderEnum order)
	{
		OrderByColumnInfo info = jacksonUtil.loadObject(jsonText, OrderByColumnInfoImpl.class);
		assert info != null;
		assert info.getOrder() == order;
	}
	
	@DataProvider
	public Object[][] sortOrders()
	{
		return new Object[][] {
			{"{\"order\":\"Ascending\",\"measureIndex\":0,\"columnValues\":[{\"total\":false,\"valueType\":\"java.lang.Integer\",\"value\":\"3\"}]}", SortOrderEnum.ASCENDING},
			{"{\"order\":\"Descending\",\"measureIndex\":0,\"columnValues\":[{\"total\":false,\"valueType\":\"java.lang.Integer\",\"value\":\"3\"}]}", SortOrderEnum.DESCENDING},
			{"{\"order\":\"ASCENDING\",\"measureIndex\":0,\"columnValues\":[{\"total\":false,\"valueType\":\"java.lang.Integer\",\"value\":\"3\"}]}", SortOrderEnum.ASCENDING},
			{"{\"order\":\"DESCENDING\",\"measureIndex\":0,\"columnValues\":[{\"total\":false,\"valueType\":\"java.lang.Integer\",\"value\":\"3\"}]}", SortOrderEnum.DESCENDING},
		};
	}
	
	@Test(dataProvider = "invalidSortOrders")
	public void invalidSortOrderTest(String jsonText)
	{
		try
		{
			OrderByColumnInfo info = jacksonUtil.loadObject(jsonText, OrderByColumnInfo.class);
			assert false;
		}
		catch (JRRuntimeException e)
		{
			assert true;
		}
	}
	
	@DataProvider
	public Object[][] invalidSortOrders()
	{
		return new Object[][] {
			{"{\"order\":\"AScending\",\"measureIndex\":0,\"columnValues\":[{\"total\":false,\"valueType\":\"java.lang.Integer\",\"value\":\"3\"}]}"},
		};
	}

}
