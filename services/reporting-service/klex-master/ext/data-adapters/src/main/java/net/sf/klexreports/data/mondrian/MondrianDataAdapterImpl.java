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
package net.sf.klexreports.data.mondrian;

import com.fasterxml.jackson.annotation.JsonRootName;

import net.sf.klexreports.data.jdbc.JdbcDataAdapterImpl;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */

@JsonRootName(value = "mondrianDataAdapter")
public class MondrianDataAdapterImpl extends JdbcDataAdapterImpl implements
		MondrianDataAdapter {
	private String catalogURI;

	public MondrianDataAdapterImpl() {
		setName("New Mondrian Data Adapter");
	}

	@Override
	public String getCatalogURI() {
		return catalogURI;
	}

	@Override
	public void setCatalogURI(String catalogURI) {
		this.catalogURI = catalogURI;
	}

}
