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
package net.sf.klexreports.barcode4j;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.type.EnumUtil;
import net.sf.klexreports.engine.type.NamedEnum;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public enum ErrorCorrectionLevelEnum implements NamedEnum
{
	/**
	 *
	 */
	L("L"),

	/**
	 *
	 */
	M("M"),

	/**
	 *
	 */
	Q("Q"),

	/**
	 *
	 */
	H("H");

	/**
	 *
	 */
	private final transient String name;
	public static final String EXCEPTION_MESSAGE_KEY_UNKNOWN_NAME = "components.barcode4j.error.correction.level.unknown.name";

	private ErrorCorrectionLevelEnum(String name) 
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
	public final ErrorCorrectionLevel getErrorCorrectionLevel()
	{
		// not storing this as instance field as we don't want to force an ZXing dependency
		ErrorCorrectionLevel level;
		if (name.equals("L"))
		{
			level = ErrorCorrectionLevel.L;
		}
		else if (name.equalsIgnoreCase("M"))
		{
			level = ErrorCorrectionLevel.M;
		}
		else if (name.equalsIgnoreCase("Q"))
		{
			level = ErrorCorrectionLevel.Q;
		}
		else if (name.equalsIgnoreCase("H"))
		{
			level = ErrorCorrectionLevel.H;
		}
		else
		{
			// should not happen
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_UNKNOWN_NAME,
					new Object[]{name});
		}
		return level;
	}

	/**
	 *
	 */
	public static ErrorCorrectionLevelEnum getByName(String name)
	{
		return EnumUtil.getEnumByName(values(), name);
	}
	
	/**
	 *
	 */
	public static ErrorCorrectionLevelEnum getValueOrDefault(ErrorCorrectionLevelEnum value)
	{
		return value == null ? L : value;
	}
	
	@Override
	public ErrorCorrectionLevelEnum getDefault()
	{
		return L;
	}
}
