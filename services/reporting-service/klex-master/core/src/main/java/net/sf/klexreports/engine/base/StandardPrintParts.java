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
package net.sf.klexreports.engine.base;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.PrintPageFormat;
import net.sf.klexreports.engine.PrintPart;
import net.sf.klexreports.engine.PrintParts;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class StandardPrintParts implements PrintParts, Serializable
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private NavigableMap<Integer, PrintPart> parts;

	public StandardPrintParts()
	{
		this.parts = new ConcurrentSkipListMap<>();
	}
	
	@Override
	public boolean hasParts()
	{
		return !parts.isEmpty();
	}

	@Override
	public int partCount()
	{
		return parts.size();
	}

	@Override
	public boolean startsAtZero()
	{
		if (!hasParts())
		{
			return false;
		}
		
		return parts.firstKey() == 0;
	}

	@Override
	public void addPart(int pageIndex, PrintPart part)
	{
		parts.put(pageIndex, part);
	}

	@Override
	public PrintPart removePart(int pageIndex)
	{
		return parts.remove(pageIndex);
	}

	@Override
	public Iterator<Map.Entry<Integer, PrintPart>> partsIterator()
	{
		return parts.entrySet().iterator();
	}

	@Override
	public int getStartPageIndex(int partIndex)
	{
		//FIXMEBOOK faster implementation?
		int partStartPage = 0;
		Iterator<Integer> it = parts.keySet().iterator();

		int i = 0;
		while (i <= partIndex && it.hasNext())
		{
			partStartPage = it.next();
			i++;
		}
		return partStartPage;
	}

	@Override
	public int getPartIndex(int pageIndex)
	{
		//FIXMEBOOK faster implementation?
		int partIndex = 0;
		Iterator<Integer> it = parts.keySet().iterator();
		while (it.hasNext())
		{
			Integer pgIdx = it.next(); 
			if (pageIndex < pgIdx)
			{
				break;
			}
			partIndex++;
		}
		return partIndex;
	}

	@Override
	public PrintPageFormat getPageFormat(int pageIndex)
	{
		Map.Entry<Integer, PrintPart> partEntry = parts.floorEntry(pageIndex);
		return partEntry == null ? null : partEntry.getValue().getPageFormat();
	}
	
	public StandardPrintParts shallowClone()
	{
		StandardPrintParts clone = new StandardPrintParts();
		clone.parts.putAll(this.parts);
		return clone;
	}

}
