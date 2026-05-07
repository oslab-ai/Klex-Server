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

import java.util.Iterator;
import java.util.List;

import mondrian.olap.Axis;
import mondrian.olap.Hierarchy;
import mondrian.olap.Position;
import net.sf.klexreports.olap.result.JROlapHierarchy;
import net.sf.klexreports.olap.result.JROlapMemberTuple;
import net.sf.klexreports.olap.result.JROlapResultAxis;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRMondrianAxis implements JROlapResultAxis
{
	
	private final JRMondrianTuple[] tuples;
	private final JRMondrianHierarchy[] hierarchies;
	
	public JRMondrianAxis(Axis axis, Hierarchy[] axisHierarchies, JRMondrianFactory factory)
	{
		List<Position> positions = axis.getPositions();
		tuples = new JRMondrianTuple[positions.size()];
		
		int idx = 0;
		for (Iterator<Position> it = positions.iterator(); it.hasNext(); ++idx)
		{
			Position position = it.next();
			tuples[idx] = new JRMondrianTuple(position, factory);
		}
		
		hierarchies = new JRMondrianHierarchy[axisHierarchies.length];
		for (int i = 0; i < axisHierarchies.length; i++)
		{
			hierarchies[i] = new JRMondrianHierarchy(axisHierarchies[i]);
		}
	}

	@Override
	public JROlapHierarchy[] getHierarchiesOnAxis()
	{
		return hierarchies;
	}

	@Override
	public JROlapMemberTuple getTuple(int index)
	{
		if (index < 0 || index >= tuples.length)
		{
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + tuples.length);
		}
		
		return tuples[index];
	}

	@Override
	public int getTupleCount()
	{
		return tuples.length;
	}

}
