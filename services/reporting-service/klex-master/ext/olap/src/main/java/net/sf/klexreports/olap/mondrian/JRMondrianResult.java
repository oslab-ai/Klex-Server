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
package net.sf.klexreports.olap.mondrian;

import mondrian.olap.Axis;
import mondrian.olap.AxisOrdinal;
import mondrian.olap.Cell;
import mondrian.olap.Query;
import mondrian.olap.Result;
import net.sf.klexreports.olap.result.JROlapCell;
import net.sf.klexreports.olap.result.JROlapResult;
import net.sf.klexreports.olap.result.JROlapResultAxis;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRMondrianResult implements JROlapResult
{
	
	private final Result result;
	private final JRMondrianAxis[] axes;
	
	public JRMondrianResult(Result result)
	{
		this.result = result;

		JRMondrianFactory factory = new JRMondrianFactory();

		Query query = result.getQuery();
		Axis[] resultAxes = result.getAxes();
		axes = new JRMondrianAxis[resultAxes.length];
		for (int i = 0; i < resultAxes.length; i++)
		{
			AxisOrdinal ordinal = AxisOrdinal.StandardAxisOrdinal.forLogicalOrdinal(i);
			axes[i] = new JRMondrianAxis(resultAxes[i], query.getMdxHierarchiesOnAxis(ordinal), factory);
		}
	}
	
	public Result getResult()
	{
		return result;
	}

	@Override
	public JROlapResultAxis[] getAxes()
	{
		return axes;
	}

	@Override
	public JROlapCell getCell(int[] axisPositions)
	{
		Cell dataCell = result.getCell(axisPositions);
		return new JRMondrianCell(dataCell);
	}

}
