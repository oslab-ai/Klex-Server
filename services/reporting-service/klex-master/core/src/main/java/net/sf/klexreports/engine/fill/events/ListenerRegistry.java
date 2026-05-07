/*
 * KlexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2019 TIBCO Software Inc. All rights reserved.
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
package net.sf.klexreports.engine.fill.events;

import java.util.List;
import java.util.function.Supplier;

import net.sf.klexreports.util.CachingSupplier;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ListenerRegistry<T>
{

	private final List<RegisteredListener<T>> listeners;
	
	public ListenerRegistry(List<RegisteredListener<T>> listeners)
	{
		this.listeners = listeners;
	}

	public <E extends T> void triggerEvent(Class<E> eventType, Supplier<E> eventSupplier)
	{
		Supplier<E> supplier = CachingSupplier.wrap(eventSupplier);
		//TODO lucian cache listeners by event type
		listeners.stream().filter(registeredListener -> registeredListener.supports(eventType))
			.forEach(registeredListener -> registeredListener.notifyListener(supplier.get()));
	}

}
