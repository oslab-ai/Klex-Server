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
package net.sf.klexreports.engine.export.ooxml.type;

import net.sf.klexreports.engine.type.EnumUtil;
import net.sf.klexreports.engine.type.NamedEnum;

/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public enum PptxFieldTypeEnum implements NamedEnum
{
	/**
	 *
	 */
	SLIDENUM("slidenum"),

	/**
	 *
	 */
	DATETIME("datetime"),

	DATETIME1("datetime1"),
	
	DATETIME2("datetime2"),
	
	DATETIME3("datetime3"),
	
	DATETIME4("datetime4"),
	
	DATETIME5("datetime5"),
	
	DATETIME6("datetime6"),
	
	DATETIME7("datetime7"),
	
	DATETIME8("datetime8"),
	
	DATETIME9("datetime9"),
	
	DATETIME10("datetime10"),
	
	DATETIME11("datetime11"),
	
	DATETIME12("datetime12"),
	
	DATETIME13("datetime13");

	/**
	 *
	 */
	private final transient String name;

	private PptxFieldTypeEnum(String name)
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
	public static PptxFieldTypeEnum getByName(String name)
	{
		return EnumUtil.getEnumByName(values(), name);
	}
}
