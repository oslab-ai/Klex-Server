/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.components.table.fill;

import java.io.Serializable;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRPropertiesHolder;
import net.sf.klexreports.engine.klexReport;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class TableklexReport extends klexReport
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private final klexReport parentReport;
	private final TableReport tableReport;

	public TableklexReport(klexReport parentReport, TableReport baseReport, 
			Serializable compileData, JRBaseObjectFactory factory,
			String compileNameSuffix)
	{
		super(baseReport, parentReport.getCompilerClass(), compileData, factory, compileNameSuffix);
		
		this.parentReport = parentReport;
		this.tableReport = baseReport;
	}

	public klexReport getParentReport()
	{
		return parentReport;
	}

	@Override
	public JRPropertiesHolder getParentProperties()
	{
		// the subreport created for the table inherits all properties from the containing report
		return parentReport;
	}

	public TableReport getBaseReport()
	{
		return tableReport;
	}

}
