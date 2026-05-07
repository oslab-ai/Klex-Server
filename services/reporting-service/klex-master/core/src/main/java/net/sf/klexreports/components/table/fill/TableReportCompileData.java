/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.components.table.fill;

import java.io.Serializable;
import java.util.Map;

import net.sf.klexreports.crosstabs.JRCrosstab;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.klexReport;
import net.sf.klexreports.engine.design.JRReportCompileData;

/**
 * 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class TableReportCompileData extends JRReportCompileData
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private final klexReport originialReport;

	public TableReportCompileData(klexReport originialReport)
	{
		this.originialReport = originialReport;
	}
	
	@Override
	public String getUnitName(klexReport klexReport, JRDataset dataset)
	{
		String unitName;
		if (dataset.isMainDataset())
		{
			unitName = super.getUnitName(klexReport, dataset);
		}
		else
		{
			unitName = super.getUnitName(originialReport, dataset);
		}
		return unitName;
	}

	@Override
	public String getUnitName(klexReport klexReport, JRCrosstab crosstab)
	{
		return super.getUnitName(originialReport, crosstab);
	}

	public void copyCrosstabCompileData(JRReportCompileData compileData)
	{
		Map<Integer, Serializable> crosstabCompileData = compileData.getCrosstabsCompileData();
		if (crosstabCompileData != null)
		{
			for (Map.Entry<Integer, Serializable> entry : crosstabCompileData.entrySet())
			{
				setCrosstabCompileData(entry.getKey(), entry.getValue());
			}
		}
	}
}
