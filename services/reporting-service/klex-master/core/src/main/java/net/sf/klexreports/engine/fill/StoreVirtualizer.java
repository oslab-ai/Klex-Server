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

import java.io.IOException;

import net.sf.klexreports.engine.JRVirtualizable;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class StoreVirtualizer extends JRAbstractLRUVirtualizer
{
	
	private final VirtualizerStore store;
	
	public StoreVirtualizer(int maxSize, VirtualizerStore store)
	{
		super(maxSize);

		this.store = store;
	}

	@Override
	protected void pageOut(JRVirtualizable o) throws IOException
	{
		boolean stored = store.store(o, serializer);
		if (!stored && !isReadOnly(o))
		{
			throw new IllegalStateException("Cannot virtualize data because the data for object UID \"" + o.getUID() + "\" already exists.");
		}
	}

	@Override
	protected void pageIn(JRVirtualizable o) throws IOException
	{
		store.retrieve(o, !isReadOnly(o), serializer);
	}

	@Override
	protected void dispose(String id)
	{
		store.remove(id);
	}
	
	@Override
	public void cleanup()
	{
		store.dispose();
	}
}
