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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.base.VirtualizableElementList;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class VirtualizableElementCounter extends DeepPrintElementCounter
{

	private static final VirtualizableElementCounter INSTANCE = new VirtualizableElementCounter();
	
	public static int count(JRPrintElement element)
	{
		return count(element, INSTANCE);
	}
	
	public static int count(Collection<? extends JRPrintElement> elements)
	{
		return count(elements, INSTANCE);
	}
	
	protected VirtualizableElementCounter()
	{
		super();
	}

	@Override
	protected void visitFrameElements(List<JRPrintElement> elements, AtomicInteger count)
	{
		if (!(elements instanceof VirtualizableElementList))
		{
			super.visitFrameElements(elements, count);
		}
	}
	
}
