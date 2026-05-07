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
package net.sf.klexreports.engine;

import java.awt.Color;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.sf.klexreports.engine.xml.JRXmlConstants;
import net.sf.klexreports.jackson.util.LineBoxSerializer;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public interface JRBoxContainer extends JRStyleContainer
{

	/**
	 *
	 */
	@JsonIgnore
	public Color getDefaultLineColor();

	/**
	 *
	 */
	@JsonGetter(JRXmlConstants.ELEMENT_box)
	@JsonInclude(Include.NON_EMPTY)
	@JsonSerialize(using = LineBoxSerializer.class)
	public JRLineBox getLineBox();

}
