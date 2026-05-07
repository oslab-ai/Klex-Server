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
package net.sf.klexreports.json.data;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.data.RewindableDataSourceProvider;
import net.sf.klexreports.engine.data.TextDataSourceAttributes;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JsonQLDataSourceProvider implements RewindableDataSourceProvider<JsonQLDataSource>
{

	private KlexReportsContext klexReportsContext;
	private String jsonSource;
	private String queryString;
	private TextDataSourceAttributes textAttributes;

	public JsonQLDataSourceProvider(KlexReportsContext klexReportsContext, String jsonSource, String queryString, TextDataSourceAttributes textAttributes)
	{
		this.klexReportsContext = klexReportsContext;
		this.jsonSource = jsonSource;
		this.queryString = queryString;
		this.textAttributes = textAttributes;
	}

	@Override
	public JsonQLDataSource getDataSource() throws JRException
	{
		JsonQLDataSource jsonQLDataSource = new JsonQLDataSource(klexReportsContext, jsonSource, queryString);
		jsonQLDataSource.setTextAttributes(textAttributes);
		return jsonQLDataSource;
	}

	@Override
	public void rewind()
	{
		// we don't need to do anything here
	}

}
