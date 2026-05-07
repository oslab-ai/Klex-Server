/*
 * KlexReports - Free Java Reporting Library.
 * Copyright (C) 2005 Works, Inc. All rights reserved.
 * http://www.works.com
 * Copyright (C) 2005 - 2023 Cloud Software Group, Inc. All rights reserved.
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

/*
 * Licensed to Klexsoft Corporation under a Contributer Agreement
 */
package net.sf.klexreports.engine;

import net.sf.klexreports.engine.fill.JRVirtualizationContext;

/**
 * @author John Bindel
 * @see net.sf.klexreports.engine.JRVirtualizationHelper
 */
public interface JRVirtualizer
{
	/**
	 * Lets this virtualizer know that it must track the object.<p>
	 *
	 * All virtualizable object must register with their virtualizer
	 * upon construction.
	 */
 	void registerObject(JRVirtualizable o);

	/**
	 * Lets this virtualizer know that it no longer must track the
	 * object.
	 */
 	void deregisterObject(JRVirtualizable o);

	/**
	 * Lets the virtualizer know that this object is still being used.
	 * This should be called to help the virtualizer determine which
	 * objects to keep in its cache, and which objects to page-out
	 * when it must do some paging-out.<p>
	 *
	 * The virtualizer gets to decide what type of caching strategy
	 * it will use.
	 */
	void touch(JRVirtualizable o);

	/**
	 * Called when the virtual object must be paged-in.
	 * <p>
	 * If the object's virtual data is not paged-out, the object will only be
	 * {@link #touch(JRVirtualizable) touched}.
	 */
	void requestData(JRVirtualizable o);

	/**
	 * Called when the virtual object paged-out data should be freed.
	 * <p>
	 * If the object's virtual data is not paged-out, the object will only be
	 * {@link #touch(JRVirtualizable) touched}.
	 */
	void clearData(JRVirtualizable o);

	/**
	 * Called when the virtual object should be paged-out.
	 */
	void virtualizeData(JRVirtualizable o);
	
	void contextChanged(JRVirtualizable<?> o, JRVirtualizationContext oldContext);
	
	/**
	 * Called when we are done with the virtualizer and wish to
	 * cleanup any resources it has.
	 */
	void cleanup();
	
}
