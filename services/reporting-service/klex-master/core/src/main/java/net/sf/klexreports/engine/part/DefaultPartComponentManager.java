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
package net.sf.klexreports.engine.part;

import net.sf.klexreports.engine.KlexReportsContext;

/**
 * A default {@link PartComponentManager component manager} implementation.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DefaultPartComponentManager implements PartComponentManager
{

	private PartComponentCompiler componentCompiler;
	private PartComponentFillFactory componentFillFactory;

	@Override
	public PartComponentFillFactory getComponentFillFactory(KlexReportsContext klexReportsContext)
	{
		return componentFillFactory;
	}

	/**
	 * Sets the fill component factory implementation.
	 * 
	 * @param fillFactory the fill component factory 
	 * @see #getComponentFillFactory(KlexReportsContext)
	 */
	public void setComponentFillFactory(PartComponentFillFactory fillFactory)
	{
		this.componentFillFactory = fillFactory;
	}

	
	@Override
	public PartComponentCompiler getComponentCompiler(KlexReportsContext klexReportsContext)
	{
		return componentCompiler;
	}

	/**
	 * Sets the component compiler implementation.
	 * 
	 * @param componentCompiler the component compiler
	 * @see #getComponentCompiler(KlexReportsContext)
	 */
	public void setComponentCompiler(PartComponentCompiler componentCompiler)
	{
		this.componentCompiler = componentCompiler;
	}

}
