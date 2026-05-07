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
package net.sf.klexreports.types.date;

import java.sql.Timestamp;
import java.util.TimeZone;

import net.sf.klexreports.engine.JRConstants;

/**
 * <p>Implementation of {@link DateRange} for relative range of times.</p>
 *
 * @author Sergey Prilukin
 */
public class RelativeTimestampRange extends RelativeDateRange implements TimestampRange 
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	public RelativeTimestampRange(String expression) {
		super(expression);
	}

	public RelativeTimestampRange(String expression, TimeZone timeZone, Integer weekStart) {
		super(expression, timeZone, weekStart);
	}

	@Override
	public Timestamp getStart() {
		return new Timestamp(super.getStart().getTime());
	}

	@Override
	public Timestamp getEnd() {
		return new Timestamp(super.getEnd().getTime());
	}
}
