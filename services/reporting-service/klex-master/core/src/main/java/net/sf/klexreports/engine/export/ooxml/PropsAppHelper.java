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
package net.sf.klexreports.engine.export.ooxml;

import java.io.Writer;

import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PropsAppHelper extends BaseHelper
{
	public static final String PROPERTY_APPLICATION = "Application";
	
	/**
	 * 
	 */
	public PropsAppHelper(KlexReportsContext klexReportsContext, Writer writer)
	{
		super(klexReportsContext, writer);
	}

	/**
	 *
	 */
	public void exportHeader()
	{
		write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		write("<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\">\n");
	}
	

	/**
	 * 
	 */
	public void exportProperty(String propName, String propValue)
	{
		write("  <" + propName + ">" + propValue + "</" + propName + ">\n");	
	}
	

	/**
	 *
	 */
	public void exportFooter()
	{
		write("</Properties>\n");
	}
}
