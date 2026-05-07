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
package net.sf.klexreports.export;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.util.JRLoader;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class SimpleExporterInput implements ExporterInput
{
	private List<ExporterInputItem> items;


	/**
	 * Creates an ExportInput object containing the list of {@link KlexPrint} objects to be exported. 
	 * If you need to concatenate several reports into the same document, you can use this constructor, 
	 * provided that you don't need to specify a different export configuration for each item. 
	 * Otherwise, consider using {@link #SimpleExporterInput(List)} instead.
	 */
	public static SimpleExporterInput getInstance(List<KlexPrint> klexPrintList)
	{
		return new SimpleExporterInput(getItems(klexPrintList));
	}


	/**
	 * Creates an {@link ExporterInput} object with a single item wrapping the {@link KlexPrint} object that will be exported. 
	 * If you already have a KlexPrint object, you can pass it to the exporter using this type of input.
	 */
	public SimpleExporterInput(KlexPrint klexPrint)
	{
		if (klexPrint != null)
		{
			this.items = new ArrayList<>();
			items.add(new SimpleExporterInputItem(klexPrint));
		}
	}


	/**
	 * Creates an {@link ExporterInput} object with a single {@link KlexPrint} item read from the provided input stream. 
	 * If you want to read the KlexPrint object from an input stream (like a web location), you can pass the stream to this constructor.
	 */
	public SimpleExporterInput(InputStream inputStream)
	{
		if (inputStream != null)
		{
			KlexPrint klexPrint = null;
			try
			{
				klexPrint = (KlexPrint)JRLoader.loadObject(inputStream);
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
			this.items = new ArrayList<>();
			items.add(new SimpleExporterInputItem(klexPrint));
		}
	}


	/**
	 * Creates an {@link ExporterInput} object with a single {@link KlexPrint} item read from the provided URL. 
	 * If the KlexPrint object is available as a web resource, you can use this constructor, instead of opening 
	 * a HTTP connection and read from the input stream.
	 */
	public SimpleExporterInput(URL url)
	{
		if (url != null)
		{
			KlexPrint klexPrint = null;
			try
			{
				klexPrint = (KlexPrint)JRLoader.loadObject(url);
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
			this.items = new ArrayList<>();
			items.add(new SimpleExporterInputItem(klexPrint));
		}
	}


	/**
	 * Creates an {@link ExporterInput} object with a single {@link KlexPrint} item read from the provided <tt>java.io.File</tt>. 
	 * This is useful if the KlexPrint object is representing a file on disk.
	 */
	public SimpleExporterInput(File file)
	{
		if (file != null)
		{
			KlexPrint klexPrint = null;
			try
			{
				klexPrint = (KlexPrint)JRLoader.loadObject(file);
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
			this.items = new ArrayList<>();
			items.add(new SimpleExporterInputItem(klexPrint));
		}
	}

	
	/**
	 * Creates an {@link ExporterInput} object with a single {@link KlexPrint} item read from the provided file. 
	 * This is useful if the KlexPrint object is representing a file on disk.
	 */
	public SimpleExporterInput(String fileName)
	{
		if (fileName != null)
		{
			KlexPrint klexPrint = null;
			try
			{
				klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(fileName);
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
			this.items = new ArrayList<>();
			items.add(new SimpleExporterInputItem(klexPrint));
		}
	}

	
	/**
	 * Creates an {@link ExporterInput} object with the provided export items.
	 */
	public SimpleExporterInput(List<ExporterInputItem> items)
	{
		this.items = items;
	}


	@Override
	public List<ExporterInputItem> getItems()
	{
		return items;
	}

	
	/**
	 * 
	 */
	protected static List<ExporterInputItem> getItems(List<KlexPrint> klexPrintList)
	{
		List<ExporterInputItem> items = null;
		
		if (klexPrintList != null)
		{
			items = new ArrayList<>(klexPrintList.size());
			for (KlexPrint klexPrint : klexPrintList)
			{
				items.add(new SimpleExporterInputItem(klexPrint));
			}
		}

		return items;
	}
}
