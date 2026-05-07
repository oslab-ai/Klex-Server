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

import mondrian.olap.Hierarchy;
import mondrian.olap.Level;
import net.sf.klexreports.olap.result.JROlapHierarchy;
import net.sf.klexreports.olap.result.JROlapHierarchyLevel;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRMondrianHierarchy implements JROlapHierarchy
{
	
	private final Hierarchy hierarchy;
	private final JRMondrianLevel[] levels;
	
	public JRMondrianHierarchy(Hierarchy hierarchy)
	{
		this.hierarchy = hierarchy;
		
		if (hierarchy == null)
		{
			levels = new JRMondrianLevel[0];
		}
		else
		{
			Level[] hierarchyLevels = hierarchy.getLevels();
			levels = new JRMondrianLevel[hierarchyLevels.length];
			for (int i = 0; i < hierarchyLevels.length; i++)
			{
				levels[i] = new JRMondrianLevel(hierarchyLevels[i]);
			}
		}
	}

	@Override
	public String getDimensionName()
	{
		return hierarchy == null ? null : hierarchy.getDimension().getName();
	}

	@Override
	public JROlapHierarchyLevel[] getLevels()
	{
		return levels;
	}

	// MPenningroth 21-April-2009 deal with case when dimension is <dimension>.<hierarchy> form
	@Override
	public String getHierarchyUniqueName()
	{
		return hierarchy == null ? null : hierarchy.getUniqueName();
	}

}
