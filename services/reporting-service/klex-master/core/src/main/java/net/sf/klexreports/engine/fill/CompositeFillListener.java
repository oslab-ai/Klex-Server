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
package net.sf.klexreports.engine.fill;

import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.engine.KlexPrint;

/**
 * {@link FillListener} implementation that contains several other listeners.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class CompositeFillListener implements FillListener
{

	public static FillListener addListener(FillListener existingListener, FillListener listener)
	{
		if (existingListener == null)
		{
			return listener;
		}
		
		if (listener == null)
		{
			return existingListener;
		}
		
		if (existingListener instanceof CompositeFillListener)
		{
			((CompositeFillListener) existingListener).listeners.add(listener);
			return existingListener;
		}
		
		CompositeFillListener newListener = new CompositeFillListener();
		newListener.listeners.add(existingListener);
		newListener.listeners.add(listener);
		return newListener;
	}
	
	private final List<FillListener> listeners = new ArrayList<>();
	
	@Override
	public void pageGenerated(KlexPrint klexPrint, int pageIndex)
	{
		for (FillListener listener : listeners)
		{
			listener.pageGenerated(klexPrint, pageIndex);
		}
	}

	@Override
	public void pageUpdated(KlexPrint klexPrint, int pageIndex)
	{
		for (FillListener listener : listeners)
		{
			listener.pageUpdated(klexPrint, pageIndex);
		}
	}

}
