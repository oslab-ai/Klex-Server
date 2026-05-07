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
package net.sf.klexreports.engine.scriptlets;

import java.util.Map;

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ScriptletFactoryContext
{

	/**
	 *
	 */
	private KlexReportsContext klexReportsContext;
	private KlexReport klexReport;
	private JRDataset dataset;
	private Map<String,Object> parameterValues;

	/**
	 *
	 */
	public ScriptletFactoryContext(
		KlexReportsContext klexReportsContext,
		JRDataset dataset,
		Map<String,Object> parameterValues 
		)
	{
		this.klexReportsContext = klexReportsContext;
		this.klexReport = (KlexReport)parameterValues.get(JRParameter.KLEX_REPORT);
		this.dataset = dataset;
		this.parameterValues = parameterValues;
	}

	/**
	 * 
	 */
	public KlexReportsContext getKlexReportsContext()
	{
		return klexReportsContext;
	}

	/**
	 * Returns the {@link KlexReport} object for this context.
	 * 
	 * <p>
	 * Note that this context might correspond to a subdataset in the report.
	 * Use {@link #getDataset()} to retrieve the dataset for which scriptlets
	 * are to be created
	 * </p>
	 * 
	 * @return the current {@link KlexReport} object
	 * @see #getDataset()
	 */
	public KlexReport getKlexReport()
	{
		return klexReport;
	}
	
	/**
	 *
	 */
	public Map<String,Object> getParameterValues()
	{
		return parameterValues;
	}
	
	/**
	 * Returns the dataset for which scriptlets are to be created.
	 * 
	 * @return a dataset
	 */
	public JRDataset getDataset()
	{
		return dataset;
	}
}
