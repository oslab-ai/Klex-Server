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
package net.sf.klexreports.engine.fill;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import net.sf.klexreports.engine.JRBiConsumer;
import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRGroup;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.design.JRDesignGroup;
import net.sf.klexreports.repo.RepositoryContext;
import net.sf.klexreports.repo.SimpleRepositoryContext;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DatasetExecution
{

	private KlexReport report;
	private Map<String, Object> parameterValues;
	private JRFillDataset fillDataset;

	public DatasetExecution(RepositoryContext repositoryContext, KlexReport report, Map<String,Object> parameters)
	{
		this.report = report;
		parameterValues = parameters == null ? new HashMap<>() : new HashMap<>(parameters);
		parameterValues.put(JRParameter.KLEX_REPORT, report);
		
		ObjectFactory factory = new ObjectFactory();
		JRDataset reportDataset = report.getMainDataset();
		fillDataset = factory.getDataset(reportDataset);
		
		@SuppressWarnings("deprecation")
		KlexReportsContext depContext = 
			net.sf.klexreports.engine.util.LocalKlexReportsContext.getLocalContext(repositoryContext.getKlexReportsContext(), parameters);
		RepositoryContext fillRepositoryContext = depContext == repositoryContext.getKlexReportsContext() ? repositoryContext
				: SimpleRepositoryContext.of(depContext, repositoryContext.getResourceContext());
		fillDataset.setRepositoryContext(fillRepositoryContext);
	}

	public void evaluateParameters(BiConsumer<JRParameter, Object> parameterConsumer) throws JRException
	{
		try
		{
			runWithParameters(() -> 
			{
				JRParameter[] parameters = fillDataset.getParameters();
				for (int i = 0; i < parameters.length; i++)
				{
					JRParameter param = parameters[i];
					Object value = fillDataset.getParameterValue(param.getName());
					parameterConsumer.accept(param, value);
				}
				return null;
			});
		}
		catch (JRException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new JRRuntimeException(e);
		}
	}
	
	protected <R> R runWithParameters(Callable<R> action) throws Exception
	{
		fillDataset.createCalculator(report);
		fillDataset.initCalculator();

		JRResourcesFillUtil.ResourcesFillContext resourcesContext = 
			JRResourcesFillUtil.setResourcesFillContext(parameterValues);
		try
		{
			fillDataset.setParameterValues(parameterValues);
			
			return action.call();
		}
		finally
		{
			fillDataset.disposeParameterContributors();
			JRResourcesFillUtil.revertResourcesFillContext(resourcesContext);
		}		
	}
	
	public void evaluateDataSource(JRBiConsumer<JRDataSource, Map<String, Object>> dataSourceConsumer) throws Exception
	{
		runWithParameters(() ->
		{
			try
			{
				fillDataset.evaluateFieldProperties();
				fillDataset.initDatasource();
				
				dataSourceConsumer.accept(fillDataset.dataSource, parameterValues);
				return null;
			}
			finally
			{
				fillDataset.closeDatasource();
			}
		});
	}

	protected static class ObjectFactory extends JRFillObjectFactory
	{
		protected ObjectFactory()
		{
			super((JRBaseFiller) null, null);
		}

		@Override
		public JRFillGroup getGroup(JRGroup group)
		{
			JRDesignGroup dummyGroup = new JRDesignGroup();
			dummyGroup.setName("DUMMY_GROUP");
			return super.getGroup(dummyGroup);
		}
	}
	
}
