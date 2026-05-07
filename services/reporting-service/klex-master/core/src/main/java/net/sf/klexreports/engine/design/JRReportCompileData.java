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
package net.sf.klexreports.engine.design;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.crosstabs.JRCrosstab;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReport;

/**
 * Structure used to hold a report's expression evaluator compile data.
 * <p>
 * An instantce consists of expression evaluators for the main report dataset
 * and for sub datasets.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * 
 * @see net.sf.klexreports.engine.KlexReport#getCompileData()
 */
public class JRReportCompileData implements Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public static final String EXCEPTION_MESSAGE_KEY_COMPILE_DATA_FOR_CROSSTAB_NOT_FOUND = "design.compile.data.for.crosstab.not.found";
	public static final String EXCEPTION_MESSAGE_KEY_COMPILE_DATA_FOR_DATASET_NOT_FOUND = "design.compile.data.for.dataset.not.found";

	/**
	 * Main report dataset compile data.
	 */
	private Serializable mainDatasetCompileData;
	
	/**
	 * Map containing compiled data per sub dataset name.
	 */
	private Map<String, Serializable> datasetCompileData;
	
	/**
	 * Map containing compiled data per crosstab name.
	 */
	private Map<Integer, Serializable> crosstabCompileData;
	
	
	/**
	 * Default constructor.
	 */
	public JRReportCompileData()
	{
		datasetCompileData = new HashMap<>();
		crosstabCompileData = new HashMap<>();
	}
	
	
	/**
	 * Sets the main dataset compile data.
	 * 
	 * @param compileData the main dataset compile data
	 */
	public void setMainDatasetCompileData(Serializable compileData)
	{
		mainDatasetCompileData = compileData;
	}
	
	
	/**
	 * Sets the compile data for a dataset.
	 * 
	 * @param dataset the dataset
	 * @param compileData the compile data
	 */
	public void setDatasetCompileData(JRDataset dataset, Serializable compileData)
	{
		if (dataset.isMainDataset())
		{
			setMainDatasetCompileData(compileData);
		}
		else
		{
			datasetCompileData.put(dataset.getName(), compileData);
		}
	}
	
	
	/**
	 * Sets the compile data for a crosstab.
	 * 
	 * @param crosstabId the generated crosstab Id, which will be used to retreive the crosstab compile data at fill time.
	 * @param compileData the compile data
	 */
	public void setCrosstabCompileData(int crosstabId, Serializable compileData)
	{
		crosstabCompileData.put(crosstabId, compileData);
	}
	
	
	/**
	 * Returns the compile data for the main dataset.
	 * 
	 * @return the compile data for the main dataset
	 */
	public Serializable getMainDatasetCompileData()
	{
		return mainDatasetCompileData;
	}
	
	
	/**
	 * Returns the compile data for a dataset.
	 * 
	 * @param dataset the dataset
	 * @return the compile data
	 * @throws JRException
	 */
	public Serializable getDatasetCompileData(JRDataset dataset) throws JRException
	{
		Serializable compileData;
		if (dataset.isMainDataset())
		{
			compileData = getMainDatasetCompileData();
		}
		else
		{
			compileData = datasetCompileData.get(dataset.getName());
			if (compileData == null)
			{
				throw 
					new JRException(
						EXCEPTION_MESSAGE_KEY_COMPILE_DATA_FOR_DATASET_NOT_FOUND,
						new Object[]{dataset.getName()});
			}
		}
		
		return compileData;
	}
	
	
	/**
	 * Returns the compile data for a crosstab.
	 * 
	 * @param crosstab the crosstab
	 * @return the compile data
	 * @throws JRException
	 */
	public Serializable getCrosstabCompileData(JRCrosstab crosstab) throws JRException
	{
		Serializable compileData = crosstabCompileData.get(crosstab.getId());
		if (compileData == null)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_COMPILE_DATA_FOR_CROSSTAB_NOT_FOUND,
					(Object[])null);
		}
		
		return compileData;
	}

	public String getUnitName(KlexReport klexReport, JRDataset dataset)
	{
		return JRAbstractCompiler.getUnitName(klexReport, dataset);
	}

	public String getUnitName(KlexReport klexReport, JRCrosstab crosstab)
	{
		return JRAbstractCompiler.getUnitName(klexReport, crosstab);
	}

	public Map<Integer, Serializable> getCrosstabsCompileData()
	{
		return crosstabCompileData;
	}
}
