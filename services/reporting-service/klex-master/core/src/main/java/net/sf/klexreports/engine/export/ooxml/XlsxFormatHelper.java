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
package net.sf.klexreports.engine.export.ooxml;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class XlsxFormatHelper extends BaseHelper
{
	private Map<String,Integer> formatCache = new HashMap<>();//FIXMEXLSX use soft cache? check other exporter caches as well

	/**
	 *
	 */
	public XlsxFormatHelper(KlexReportsContext klexReportsContext, Writer writer)
	{
		super(klexReportsContext, writer);
	}
	
	/**
	 *
	 */
	public int getFormat(String pattern)
	{
		if (pattern == null)
		{
			return -1;			
		}
		
		XlsxFormatInfo formatInfo = new XlsxFormatInfo(pattern);
		Integer formatIndex = formatCache.get(formatInfo.getId());
		if (formatIndex == null)
		{
			formatIndex = formatCache.size();
			export(formatInfo, formatIndex);
			formatCache.put(formatInfo.getId(), formatIndex);
		}
		return formatIndex;
	}

	/**
	 *
	 */
	private void export(XlsxFormatInfo formatInfo, Integer formatIndex)
	{
		write("<numFmt numFmtId=\"" + (formatIndex + 1) + "\"");
		if (formatInfo.pattern != null && formatInfo.pattern.trim().length() > 0)
		{
			write(" formatCode=\"" + formatInfo.pattern + "\"");
		}
		else
		{
			write(" formatCode=\"General\"");
		}
		write("/>\n");
	}

}
