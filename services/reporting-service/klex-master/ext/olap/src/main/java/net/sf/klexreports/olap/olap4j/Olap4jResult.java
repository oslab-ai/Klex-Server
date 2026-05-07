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
package net.sf.klexreports.olap.olap4j;

import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.olap.result.JROlapCell;
import net.sf.klexreports.olap.result.JROlapResult;
import net.sf.klexreports.olap.result.JROlapResultAxis;

import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.CellSetAxis;


/**
 * @author swood
 */
public class Olap4jResult implements JROlapResult
{
	
	private CellSet cellSet = null;
	private Olap4jResultAxis[] axes;

	public Olap4jResult(CellSet cSet)
	{
		this.cellSet = cSet;
		
		Olap4jFactory factory = new Olap4jFactory();

		List<CellSetAxis> resultAxes = cellSet.getAxes();
		axes = new Olap4jResultAxis[resultAxes.size()];
		for (int i = 0; i < resultAxes.size(); i++)
		{
			// AxisOrdinal ordinal = AxisOrdinal.StandardAxisOrdinal.forLogicalOrdinal(i);
			axes[i] = new Olap4jResultAxis(cellSet.getAxes().get(i),
					cellSet.getAxes().get(i).getAxisMetaData().getHierarchies(),
					factory);
		}
	}

	@Override
	public JROlapResultAxis[] getAxes()
	{
		return axes;
	}

	@Override
	public JROlapCell getCell(int[] axisPositions)
	{
		List<Integer> positions = new ArrayList<>(axisPositions.length);
		for (int index = 0; index < axisPositions.length; index++)
		{
			positions.add(axisPositions[index]);
		} 
		Cell dataCell = cellSet.getCell(positions);
		return new Olap4jCell(dataCell);
	}
}
