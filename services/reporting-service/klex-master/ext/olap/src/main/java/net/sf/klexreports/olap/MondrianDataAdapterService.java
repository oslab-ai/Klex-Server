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
package net.sf.klexreports.olap;

import java.util.Locale;
import java.util.Map;

import mondrian.olap.Connection;
import mondrian.olap.DriverManager;
import mondrian.olap.Util;
import net.sf.klexreports.data.jdbc.JdbcDataAdapterService;
import net.sf.klexreports.data.mondrian.MondrianDataAdapter;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.ParameterContributorContext;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class MondrianDataAdapterService extends JdbcDataAdapterService {

	private Connection connection = null;

	/**
	 * 
	 */
	public MondrianDataAdapterService(
			ParameterContributorContext paramContribContext,
			MondrianDataAdapter jdbcDataAdapter) {
		super(paramContribContext, jdbcDataAdapter);
	}

	@Override
	public MondrianDataAdapter getJdbcDataAdapter() {
		return (MondrianDataAdapter) getDataAdapter();
	}

	@Override
	public void contributeParameters(Map<String, Object> parameters)
			throws JRException {
		MondrianDataAdapter mda = getJdbcDataAdapter();
		if (mda != null) {
			Util.PropertyList props = new Util.PropertyList();
			props.put("Catalog", mda.getCatalogURI());
			props.put("Provider", "mondrian");
			props.put("Locale", Locale.getDefault().getLanguage());

			connection = DriverManager.getConnection(props, null,
					new SimpleSQLDataSource(this));

			parameters
					.put(JRMondrianQueryExecuterFactory.PARAMETER_MONDRIAN_CONNECTION,
							connection);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (connection != null) {
			connection.close();
		}
	}
}
