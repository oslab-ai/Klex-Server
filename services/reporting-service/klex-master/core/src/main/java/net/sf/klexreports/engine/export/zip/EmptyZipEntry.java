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
package net.sf.klexreports.engine.export.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import net.sf.klexreports.engine.JRRuntimeException;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class EmptyZipEntry implements ExportZipEntry 
{
	public static final String EXCEPTION_MESSAGE_KEY_FORBIDDEN_METHOD_CALL = "export.zip.forbidden.method.call";

	/**
	 * 
	 */
	private String name;
	
	/**
	 * 
	 */
	public EmptyZipEntry(String name)
	{
		this.name = name;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public Writer getWriter()
	{
		throw 
			new JRRuntimeException(
				EXCEPTION_MESSAGE_KEY_FORBIDDEN_METHOD_CALL,
				(Object[])null);
	}
	
	@Override
	public OutputStream getOutputStream()
	{
		throw 
			new JRRuntimeException(
				EXCEPTION_MESSAGE_KEY_FORBIDDEN_METHOD_CALL,
				(Object[])null);
	}
	
	@Override
	public void writeData(OutputStream os) throws IOException
	{
	}
	
	@Override
	public void dispose()
	{
	}
	
}
