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
package net.sf.klexreports.charts.type;

import net.sf.klexreports.engine.type.EnumUtil;
import net.sf.klexreports.engine.type.NamedEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public enum TimePeriodEnum implements NamedEnum
{
	/**
	 *
	 */
	YEAR("Year"),

	/**
	 *
	 */
	QUARTER("Quarter"),
	
	/**
	 *
	 */
	MONTH("Month"),
	
	/**
	 *
	 */
	WEEK("Week"),
	
	/**
	 *
	 */
	DAY("Day"),
	
	/**
	 *
	 */
	HOUR("Hour"),
	
	/**
	 *
	 */
	MINUTE("Minute"),
	
	/**
	 *
	 */
	SECOND("Second"),
	
	/**
	 *
	 */
	MILLISECOND("Millisecond");


	/**
	 *
	 */
	private final transient String name;

	private TimePeriodEnum(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	/**
	 *
	 */
	public static TimePeriodEnum getByName(String name)
	{
		if ("Milisecond".equals(name)) // deal with historical spelling error
		{
			return MILLISECOND;
		}
		return EnumUtil.getEnumByName(values(), name);
	}
	
	@Override
	public TimePeriodEnum getDefault()
	{
		return DAY;
	}
}
