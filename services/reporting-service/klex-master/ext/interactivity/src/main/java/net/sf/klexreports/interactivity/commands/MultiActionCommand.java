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

import java.util.List;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.interactivity.actions.AbstractAction;
import net.sf.klexreports.interactivity.actions.ActionException;


/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class MultiActionCommand implements Command {
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private KlexReportsContext klexReportsContext;
	private ReportContext reportContext;
	private List<AbstractAction> actions;
	private CommandStack individualResizeCommandStack;
	
	public MultiActionCommand(List<AbstractAction> actions, KlexReportsContext klexReportsContext, ReportContext reportContext) {
		this.actions = actions;
		this.klexReportsContext = klexReportsContext;
		this.reportContext = reportContext;
		this.individualResizeCommandStack = new CommandStack();
	}

	@Override
	public void execute() throws CommandException {
		if (actions != null) {
			for (AbstractAction action: actions) {
				action.init(klexReportsContext, reportContext);
				action.setCommandStack(individualResizeCommandStack);
				try {
					action.run();
				} catch (ActionException e) {
					throw new CommandException(e);
				}
			}
		}
	}

	@Override
	public void undo() {
		individualResizeCommandStack.undoAll();
	}

	@Override
	public void redo() {
		individualResizeCommandStack.redoAll();
	}

}
