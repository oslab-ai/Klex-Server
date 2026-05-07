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
package net.sf.klexreports.jakarta.ejbql;

import java.util.Map;

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.query.AbstractQueryExecuterFactory;
import net.sf.klexreports.engine.query.EjbqlConstants;
import net.sf.klexreports.engine.query.JRQueryExecuter;
import net.sf.klexreports.engine.util.Designated;

/**
 * Java Persistence API query executer factory for EJBQL queries.
 * <p/>
 * The factory creates {@link net.sf.klexreports.jakarta.ejbql.JRJpaQueryExecuter JRJpaQueryExecuter}
 * query executers. 
 * 
 * @author Marcel Overdijk (marceloverdijk@hotmail.com)
 */
public class JRJpaQueryExecuterFactory extends AbstractQueryExecuterFactory implements Designated 
{
	private static final Object[] JPA_BUILTIN_PARAMETERS = {
		EjbqlConstants.PARAMETER_JPA_ENTITY_MANAGER,  "jakarta.persistence.EntityManager",
		EjbqlConstants.PARAMETER_JPA_QUERY_HINTS_MAP, "java.util.Map"
		};	
		
	@Override
	public Object[] getBuiltinParameters() {
		return JPA_BUILTIN_PARAMETERS;
	}
	
	@Override
	public JRQueryExecuter createQueryExecuter(
		KlexReportsContext klexReportsContext,
		JRDataset dataset, 
		Map<String,? extends JRValueParameter> parameters
		) throws JRException 
	{
		return new JRJpaQueryExecuter(klexReportsContext, dataset, parameters);
	}

	/**
	 * Returns <code>true</code> for all parameter types.
	 */
	@Override
	public boolean supportsQueryParameterType(String className) {
		return true;
	}

	@Override
	public String getDesignation()
	{
		return EjbqlConstants.QUERY_EXECUTER_NAME_EJBQL;
	}
}
