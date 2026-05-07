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
package net.sf.klexreports.engine.util;

import java.util.Iterator;
import java.util.Map;

import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.PrintPart;
import net.sf.klexreports.engine.PrintParts;
import net.sf.klexreports.engine.base.StandardPrintParts;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class PartsUtil
{

	public static PartsUtil instance(KlexReportsContext klexReportsContext)
	{
		return new PartsUtil(klexReportsContext);
	}
	
	private final KlexReportsContext klexReportsContext;

	public PartsUtil(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	public PrintParts getVisibleParts(KlexPrint klexPrint)
	{
		StandardPrintParts visibleParts = new StandardPrintParts();
		if (klexPrint.hasParts())
		{
			JRPropertiesUtil properties = JRPropertiesUtil.getInstance(klexReportsContext);
			for (Iterator<Map.Entry<Integer, PrintPart>> iterator = klexPrint.getParts().partsIterator(); iterator.hasNext();)
			{
				Map.Entry<Integer, PrintPart> partEntry = iterator.next();
				PrintPart part = partEntry.getValue();
				if (properties.getBooleanProperty(part, PrintPart.PROPERTY_VISIBLE, true))
				{
					visibleParts.addPart(partEntry.getKey(), part);
				}
			}		
		}
		return visibleParts;
	}
	
}
