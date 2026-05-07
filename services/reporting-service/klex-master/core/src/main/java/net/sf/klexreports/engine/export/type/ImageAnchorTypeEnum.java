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
package net.sf.klexreports.engine.export.type;

import net.sf.klexreports.engine.type.EnumUtil;
import net.sf.klexreports.engine.type.NamedEnum;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public enum ImageAnchorTypeEnum implements NamedEnum
{
	/**
	 * Constant useful for specifying the <code>Move and size with cells</code> anchor type in Excel.
	 */
	MOVE_SIZE("MoveSize"),

	/**
	 * Constant useful for specifying the <code>Move but don't size with cells</code> anchor type in Excel.
	 */
	MOVE_NO_SIZE("MoveNoSize"),
	
	/**
	 * Constant useful for specifying the <code>Don't move or size with cells</code> anchor type in Excel.
	 */
	NO_MOVE_NO_SIZE("NoMoveNoSize");
	
	
	/**
	 *
	 */
	private final transient String name;

	private ImageAnchorTypeEnum(String name)
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
	public static ImageAnchorTypeEnum getByName(String name)
	{
		return EnumUtil.getEnumByName(values(), name);
	}
}
