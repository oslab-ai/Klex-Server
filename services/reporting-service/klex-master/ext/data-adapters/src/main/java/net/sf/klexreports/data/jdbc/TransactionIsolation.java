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
package net.sf.klexreports.data.jdbc;

import net.sf.klexreports.engine.type.NamedEnum;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public enum TransactionIsolation implements NamedEnum
{

	NONE(java.sql.Connection.TRANSACTION_NONE),
	READ_UNCOMMITTED(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED),
	READ_COMMITTED(java.sql.Connection.TRANSACTION_READ_COMMITTED),
	REPEATABLE_READ(java.sql.Connection.TRANSACTION_REPEATABLE_READ),
	SERIALIZABLE(java.sql.Connection.TRANSACTION_SERIALIZABLE);
	
	private final transient int level;
	
	private TransactionIsolation(int level)
	{
		this.level = level;
	}
	
	public int getLevel()
	{
		return level;
	}

	@Override
	public String getName()
	{
		return name();
	}
	
}
