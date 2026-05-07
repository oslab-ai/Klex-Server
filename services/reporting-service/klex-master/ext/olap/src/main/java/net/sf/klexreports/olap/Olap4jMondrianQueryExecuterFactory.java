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

import java.util.Map;

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.AbstractQueryExecuterFactory;
import net.sf.klexreports.engine.query.JRQueryExecuter;


/**
 * @author swood
 */
public class Olap4jMondrianQueryExecuterFactory extends AbstractQueryExecuterFactory
{
	public final static String PARAMETER_JDBC_DRIVERS = "JdbcDrivers";
	public final static String PARAMETER_JDBC_URL = "JdbcUrl";
	public final static String PARAMETER_JDBC_USER = "JdbcUser";
	public final static String PARAMETER_JDBC_PASSWORD = "JdbcPassword";
	public final static String PARAMETER_CATALOG = "Catalog";
	
	private final static Object[] MONDRIAN_OLAP4J_BUILTIN_PARAMETERS = {
		PARAMETER_JDBC_DRIVERS,  "java.lang.String",
		PARAMETER_JDBC_URL,  "java.lang.String",
		PARAMETER_JDBC_USER,  "java.lang.String",
		PARAMETER_JDBC_PASSWORD,  "java.lang.String",
		PARAMETER_CATALOG,  "java.lang.String",
		};
	
	@Override
	public Object[] getBuiltinParameters()
	{
		return MONDRIAN_OLAP4J_BUILTIN_PARAMETERS;
	}

	@Override
	public JRQueryExecuter createQueryExecuter(
		KlexReportsContext klexReportsContext, 
		JRDataset dataset, 
		Map<String,? extends JRValueParameter> parameters
		) throws JRException
	{
		return new JRMondrianQueryExecuter(klexReportsContext, dataset, parameters);
	}

	@Override
	public boolean supportsQueryParameterType(String className)
	{
		return true;
	}
}
