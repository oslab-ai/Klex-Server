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
import java.util.Map;

import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRStringUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class XlsxWorkbookHelper extends BaseHelper
{
	StringBuilder definedNames;
	
	/**
	 * 
	 */
	public XlsxWorkbookHelper(KlexReportsContext klexReportsContext, Writer writer)
	{
		super(klexReportsContext, writer);
	}
	
	/**
	 * 
	 */
	public XlsxWorkbookHelper(KlexReportsContext klexReportsContext, Writer writer, StringBuilder definedNames)
	{
		super(klexReportsContext, writer);
		this.definedNames = definedNames;
	}

	/**
	 *
	 */
	public void exportHeader()
	{
		write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		write("<workbook\n");
		write(" xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"\n");
		write(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n");
		write("<workbookPr filterPrivacy=\"1\" codeName=\"ThisWorkbook\"/>\n");
		write("<sheets>\n");
	}
	

	/**
	 * 
	 */
	public void exportSheet(int index, String name, Map<String, Integer> sheetMapping)
	{
		String sheetName = JRStringUtil.xmlEncode(name);
		sheetMapping.put(sheetName, index-1);
		write("  <sheet name=\"" + sheetName + "\" sheetId=\"" + index + "\" r:id=\"rId" + index + "\"/>\n");
	}
	

	/**
	 *
	 */
	public void exportFooter()
	{
		write("</sheets>\n");
		if(definedNames != null && definedNames.length() > 0) {
			write("<definedNames>\n");
			write(definedNames.toString());
			write("</definedNames>\n");
		}
		write("</workbook>\n");
	}
}
