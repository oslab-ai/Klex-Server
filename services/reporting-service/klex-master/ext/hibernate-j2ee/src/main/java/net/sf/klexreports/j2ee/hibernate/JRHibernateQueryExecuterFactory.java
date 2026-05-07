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
package net.sf.klexreports.j2ee.hibernate;

import java.util.Map;

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.AbstractQueryExecuterFactory;
import net.sf.klexreports.engine.query.HibernateConstants;
import net.sf.klexreports.engine.query.JRQueryExecuter;
import net.sf.klexreports.engine.util.Designated;

/**
 * Query executer factory for HQL queries that uses Hibernate 3.
 * <p/>
 * The factory creates {@link net.sf.klexreports.j2ee.hibernate.JRHibernateQueryExecuter JRHibernateQueryExecuter}
 * query executers. 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRHibernateQueryExecuterFactory extends AbstractQueryExecuterFactory implements Designated
{
	
	/**
	 * Built-in parameter used for collection filter queries.
	 * <p/>
	 * The value of this parameter will be used as the collection to filter using the query.
	 */
	public final static String PARAMETER_HIBERNATE_FILTER_COLLECTION = "HIBERNATE_FILTER_COLLECTION";
	
	public final static Object[] HIBERNATE_BUILTIN_PARAMETERS = {
		//passing the parameter type as class name and not class in order to 
		//avoid a dependency on Hibernate classes so that reports that have
		//HQL queries would load even when Hibernate is not present
		HibernateConstants.PARAMETER_HIBERNATE_SESSION,  "org.hibernate.Session",
		PARAMETER_HIBERNATE_FILTER_COLLECTION,  "java.lang.Object",
		};
	
	/**
	 * Returns an array containing the {@link HibernateConstants#PARAMETER_HIBERNATE_SESSION PARAMETER_HIBERNATE_SESSION} and
	 * {@link #PARAMETER_HIBERNATE_FILTER_COLLECTION PARAMETER_HIBERNATE_FILTER_COLLECTION} parameters.
	 */
	@Override
	public Object[] getBuiltinParameters()
	{
		return HIBERNATE_BUILTIN_PARAMETERS;
	}

	@Override
	public JRQueryExecuter createQueryExecuter(
		KlexReportsContext klexReportsContext, 
		JRDataset dataset, 
		Map<String, ? extends JRValueParameter> parameters
		) throws JRException
	{
		return new JRHibernateQueryExecuter(klexReportsContext, dataset, parameters);
	}

	/**
	 * Returns <code>true</code> for all parameter types.
	 */
	@Override
	public boolean supportsQueryParameterType(String className)
	{
		return true;
	}

	@Override
	public String getDesignation()
	{
		return HibernateConstants.QUERY_EXECUTER_NAME_HQL;
	}
}
