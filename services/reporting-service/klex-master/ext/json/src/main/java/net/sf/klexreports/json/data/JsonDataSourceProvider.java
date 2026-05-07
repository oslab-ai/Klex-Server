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
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JsonDataSourceProvider implements RewindableDataSourceProvider<JsonDataSource>
{

	private KlexReportsContext klexReportsContext;
	private String jsonSource;
	private String queryString;
	private TextDataSourceAttributes textAttributes;

	public JsonDataSourceProvider(KlexReportsContext klexReportsContext, String jsonSource, String queryString, TextDataSourceAttributes textAttributes)
	{
		this.klexReportsContext = klexReportsContext;
		this.jsonSource = jsonSource;
		this.queryString = queryString;
		this.textAttributes = textAttributes;
	}

	@Override
	public JsonDataSource getDataSource() throws JRException
	{
		JsonDataSource jsonDataSource = new JsonDataSource(klexReportsContext, jsonSource, queryString);
		jsonDataSource.setTextAttributes(textAttributes);
		return jsonDataSource;
	}

	@Override
	public void rewind()
	{
		// we don't need to do anything here
	}

}
