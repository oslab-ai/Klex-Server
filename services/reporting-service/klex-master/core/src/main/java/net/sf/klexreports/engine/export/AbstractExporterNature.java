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

/*
 * Contributors:
 * Greg Hilton 
 */

package net.sf.klexreports.engine.export;

import java.util.Map;

import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.KlexReportsContext;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class AbstractExporterNature implements ExporterNature
{
	protected final KlexReportsContext klexReportsContext;
	protected final GenericElementHandlerEnviroment handlerEnvironment;
	protected final JRPropertiesUtil propertiesUtil;
	protected final ExporterFilter filter;

	/**
	 * 
	 */
	protected AbstractExporterNature(
		KlexReportsContext klexReportsContext,
		ExporterFilter filter 
		)
	{
		this.klexReportsContext = klexReportsContext;
		this.handlerEnvironment = GenericElementHandlerEnviroment.getInstance(klexReportsContext);
		this.propertiesUtil = JRPropertiesUtil.getInstance(klexReportsContext);
		this.filter = filter;
	}
	
	/**
	 *
	 */
	public JRPropertiesUtil getPropertiesUtil()
	{
		return propertiesUtil;
	}

	@Override
	public void setXProperties(CutsInfo xCuts, JRPrintElement element, int row1, int col1, int row2, int col2)
	{
		// nothing to do here
	}
	
	@Override
	public void setXProperties(Map<String,Object> xCutsProperties, JRPrintElement element)
	{
		// nothing to do here
	}
	
	@Override
	public void setYProperties(CutsInfo yCuts, JRPrintElement element, int row1, int col1, int row2, int col2)
	{
		// nothing to do here
	}
	
	@Override
	public void setYProperties(Map<String,Object> yCutsProperties, JRPrintElement element)
	{
		// nothing to do here
	}
	
}
