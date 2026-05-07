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
package net.sf.klexreports.interactivity.headertoolbar.actions;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.base.JRBaseElement;
import net.sf.klexreports.interactivity.commands.Command;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class ResizeElementCommand implements Command 
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private int width;
	private int oldWidth;
	private JRBaseElement receiver;
	
	public ResizeElementCommand(JRBaseElement receiver, int width) {
		this.receiver = receiver;
		this.width = width;
		this.oldWidth = receiver.getWidth();
	}

	@Override
	public void execute() {
		receiver.setWidth(width);
	}		
	
	@Override
	public void undo() {
		receiver.setWidth(oldWidth);
	}

	@Override
	public void redo() {
		execute();
	}

}
