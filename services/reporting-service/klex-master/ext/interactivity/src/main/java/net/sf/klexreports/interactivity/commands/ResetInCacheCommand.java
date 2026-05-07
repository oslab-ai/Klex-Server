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
package net.sf.klexreports.interactivity.commands;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.repo.KlexDesignCache;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ResetInCacheCommand implements Command
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private Command command;
	private KlexReportsContext klexReportsContext;
	private ReportContext reportContext;
	private String uri;
	
	public ResetInCacheCommand(
		Command command, 
		KlexReportsContext klexReportsContext, 
		ReportContext reportContext, 
		String uri
		) 
	{
		this.command = command;
		this.klexReportsContext = klexReportsContext;
		this.reportContext = reportContext;
		this.uri = uri;
	}

	@Override
	public void execute() throws CommandException
	{
		command.execute();
		
		KlexDesignCache.getInstance(klexReportsContext, reportContext).resetKlexReport(uri);
	}
	
	@Override
	public void undo() 
	{
		command.undo();
		
		KlexDesignCache.getInstance(klexReportsContext, reportContext).resetKlexReport(uri);
	}

	@Override
	public void redo() 
	{
		command.redo();
		
		KlexDesignCache.getInstance(klexReportsContext, reportContext).resetKlexReport(uri);
	}

}
