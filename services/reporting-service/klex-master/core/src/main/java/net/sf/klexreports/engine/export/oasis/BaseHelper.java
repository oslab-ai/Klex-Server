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
package net.sf.klexreports.engine.export.oasis;

import java.io.IOException;
import java.io.Writer;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class BaseHelper
{
	/**
	 *
	 */
	protected final KlexReportsContext klexReportsContext;
	protected final Writer writer;

	/**
	 *
	 */
	public BaseHelper(KlexReportsContext klexReportsContext, Writer writer)
	{
		this.klexReportsContext = klexReportsContext;
		this.writer = writer;
	}
	
	public void write(String text)
	{
		try
		{
			writer.write(text);
		}
		catch(IOException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	public void close()
	{
		try
		{
			writer.close();
		}
		catch(IOException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	public void flush()
	{
		try
		{
			writer.flush();
		}
		catch(IOException e)
		{
			throw new JRRuntimeException(e);
		}
	}
}

