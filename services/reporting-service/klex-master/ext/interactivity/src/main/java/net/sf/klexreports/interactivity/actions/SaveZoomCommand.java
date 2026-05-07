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
package net.sf.klexreports.interactivity.actions;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.export.type.ZoomTypeEnum;
import net.sf.klexreports.interactivity.commands.Command;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class SaveZoomCommand implements Command {

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public static final String PROPERTY_VIEWER_ZOOM = "net.sf.klexreports.viewer.zoom";

	private String zoomValue;
	private String oldZoomValue;
	private KlexDesign klexDesign;

	public SaveZoomCommand(KlexDesign klexDesign, String zoomValue) {
		this.zoomValue = zoomValue;
		this.klexDesign = klexDesign;
	}


	@Override
	public void execute() {
		oldZoomValue = klexDesign.getProperty(PROPERTY_VIEWER_ZOOM);
		if (oldZoomValue == null) {
			oldZoomValue = "1";
		}
		ZoomTypeEnum zoomType = ZoomTypeEnum.getByName(zoomValue);
		if (zoomType != null) {
			zoomValue = zoomType.getName();
		}
		klexDesign.setProperty(PROPERTY_VIEWER_ZOOM, zoomValue);
	}


	@Override
	public void undo() {
		klexDesign.setProperty(PROPERTY_VIEWER_ZOOM, oldZoomValue);
	}


	@Override
	public void redo() {
		klexDesign.setProperty(PROPERTY_VIEWER_ZOOM, zoomValue);
	}

}
