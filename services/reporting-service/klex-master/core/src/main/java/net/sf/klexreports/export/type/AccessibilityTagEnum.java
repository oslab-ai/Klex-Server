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
package net.sf.klexreports.export.type;

import net.sf.klexreports.engine.type.EnumUtil;
import net.sf.klexreports.engine.type.NamedEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public enum AccessibilityTagEnum implements NamedEnum
{
	/**
	 * 
	 */
	TABLE("table"),

	/**
	 * 
	 */
	TABLE_LAYOUT("table_layout"),

	/**
	 * 
	 */
	COLUMN_HEADER("columnheader"),

	/**
	 * 
	 */
	ROW_HEADER("rowheader"),

	/**
	 * 
	 */
	H1("h1"),

	/**
	 * 
	 */
	H2("h2"),

	/**
	 * 
	 */
	H3("h3"),

	/**
	 * 
	 */
	H4("h4"),

	/**
	 * 
	 */
	H5("h5"),

	/**
	 * 
	 */
	H6("h6");
	
	/**
	 *
	 */
	private final transient String name;

	private AccessibilityTagEnum(String name)
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
	public static AccessibilityTagEnum getByName(String name)
	{
		return EnumUtil.getEnumByName(values(), name);
	}
}
