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

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.util.JRLoader;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class FileBufferedZip extends AbstractZip
{
	/**
	 * 
	 */
	private final Integer memoryThreshold;

	/**
	 * 
	 */
	public FileBufferedZip()
	{
		this(null);
	}
	
	/**
	 * 
	 */
	public FileBufferedZip(Integer memoryThreshold)
	{
		this.memoryThreshold = memoryThreshold;
	}
	
	@Override
	public ExportZipEntry createEntry(String name)
	{
		ExportZipEntry entry = memoryThreshold == null ? new FileBufferedZipEntry(name) : new FileBufferedZipEntry(name, memoryThreshold);

		addEntry(entry);
		
		return entry;
	}
	
	/**
	 *
	 */
	public void addEntry(String name, String resource)
	{
		byte[] bytes = null;

		try
		{
			bytes = JRLoader.loadBytesFromResource(resource);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
		
		addEntry(new FileBufferedZipEntry(name, bytes));
	}

	/**
	 *
	 */
	public void addEntry(String name, byte[] bytes)
	{
		addEntry(new FileBufferedZipEntry(name, bytes));
	}
	
}
