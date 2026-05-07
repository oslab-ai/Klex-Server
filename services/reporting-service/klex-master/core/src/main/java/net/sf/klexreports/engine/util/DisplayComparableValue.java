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
package net.sf.klexreports.engine.util;

import net.sf.klexreports.engine.JRConstants;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DisplayComparableValue<T extends Comparable<T>> extends DisplayValue<T> 
		implements Comparable<DisplayComparableValue<T>>
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	public DisplayComparableValue(T key, String label)
	{
		super(key, label);
	}

	@Override
	public int compareTo(DisplayComparableValue<T> o)
	{
		T otherKey = o.key;
		// considering nulls greater than non null values
		int order;
		if (key == null)
		{
			if (otherKey == null)
			{
				order = 0;
			}
			else
			{
				order = -1;
			}
		}
		else
		{
			if (otherKey == null)
			{
				order = 1;
			}
			else
			{
				order = key.compareTo(otherKey);
			}
		}
		return order;
	}

}
