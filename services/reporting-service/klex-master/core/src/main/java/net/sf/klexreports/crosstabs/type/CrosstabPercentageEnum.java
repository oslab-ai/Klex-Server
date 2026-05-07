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
package net.sf.klexreports.crosstabs.type;

import net.sf.klexreports.engine.type.EnumUtil;
import net.sf.klexreports.engine.type.NamedEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public enum CrosstabPercentageEnum implements NamedEnum
{
	/**
	 * Percentage type indicating that the value will not be calculated
	 * as a percentage.
	 */
	NONE("None"),
	
	/**
	 * Percentage type indicating that the value will be calculated as percentage
	 * of the grand total value.
	 */
	GRAND_TOTAL("GrandTotal");

	/**
	 *
	 */
	private final transient String name;

	private CrosstabPercentageEnum(String name)
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
	public static CrosstabPercentageEnum getByName(String name)
	{
		return EnumUtil.getEnumByName(values(), name);
	}
	
	@Override
	public CrosstabPercentageEnum getDefault()
	{
		return NONE;
	}
}
