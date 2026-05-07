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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.zip.ExportZipEntry;
import net.sf.klexreports.engine.export.zip.FileBufferedZip;
import net.sf.klexreports.repo.RepositoryUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class XlsxZip extends FileBufferedZip
{
	public static final String EXCEPTION_MESSAGE_KEY_MACRO_TEMPLATE_NOT_FOUND = "export.xlsx.macro.template.not.found";
	
	private final RepositoryUtil repository;

	/**
	 * 
	 */
	private ExportZipEntry workbookEntry;
	private ExportZipEntry stylesEntry;
	private ExportZipEntry sharedStringsEntry;
	private ExportZipEntry relsEntry;
	private ExportZipEntry contentTypesEntry;
	private ExportZipEntry appEntry;
	private ExportZipEntry coreEntry;
	
	/**
	 * 
	 */
	public XlsxZip(KlexReportsContext klexReportsContext) throws IOException
	{
		this(klexReportsContext, null);
	}
	
	/**
	 * 
	 */
	public XlsxZip(KlexReportsContext klexReportsContext, Integer memoryThreshold) throws IOException
	{
		this(klexReportsContext, RepositoryUtil.getInstance(klexReportsContext), memoryThreshold);
	}
	
	public XlsxZip(KlexReportsContext klexReportsContext, RepositoryUtil repository, Integer memoryThreshold) throws IOException
	{
		super(memoryThreshold);

		this.repository = repository;
		
		workbookEntry = createEntry("xl/workbook.xml");
		
		stylesEntry = createEntry("xl/styles.xml");
		
		sharedStringsEntry = createEntry("xl/sharedStrings.xml");

		relsEntry = createEntry("xl/_rels/workbook.xml.rels");
		
		contentTypesEntry = createEntry("[Content_Types].xml");
		
		appEntry = createEntry("docProps/app.xml");

		coreEntry = createEntry("docProps/core.xml");

		addEntry("_rels/.rels", "net/sf/klexreports/engine/export/ooxml/xlsx/_rels/xml.rels");
	}
	
	/**
	 *
	 */
	public ExportZipEntry getWorkbookEntry()
	{
		return workbookEntry;
	}
	
	/**
	 *
	 */
	public ExportZipEntry getStylesEntry()
	{
		return stylesEntry;
	}
	
	/**
	 *
	 */
	public ExportZipEntry getSharedStringsEntry()
	{
		return sharedStringsEntry;
	}
	
	/**
	 *
	 */
	public ExportZipEntry getRelsEntry()
	{
		return relsEntry;
	}
	
	/**
	 *
	 */
	public ExportZipEntry getContentTypesEntry()
	{
		return contentTypesEntry;
	}
	
	/**
	 *
	 */
	public ExportZipEntry getAppEntry()
	{
		return appEntry;
	}
	
	/**
	 *
	 */
	public ExportZipEntry getCoreEntry()
	{
		return coreEntry;
	}
	
	/**
	 * 
	 */
	public ExportZipEntry addSheet(int index)
	{
		return createEntry("xl/worksheets/sheet" + index + ".xml");
	}
	
	/**
	 * 
	 */
	public ExportZipEntry addSheetRels(int index)
	{
		return createEntry("xl/worksheets/_rels/sheet" + index + ".xml.rels");
	}
	
	/**
	 * 
	 */
	public ExportZipEntry addDrawing(int index)
	{
		return createEntry("xl/drawings/drawing" + index + ".xml");
	}
	
	/**
	 * 
	 */
	public ExportZipEntry addDrawingRels(int index)
	{
		return createEntry("xl/drawings/_rels/drawing" + index + ".xml.rels");
	}

	/**
	 * 
	 */
	public void addMacro(String template)
	{
		InputStream templateIs = null;
		ZipInputStream templateZipIs = null;
		try
		{
			templateIs = repository.getInputStreamFromLocation(template);//TODO
			if (templateIs == null)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_MACRO_TEMPLATE_NOT_FOUND,
						new Object[]{template});
			}
			else
			{
				templateZipIs = new ZipInputStream(templateIs);
				
				ZipEntry entry = null;
				while ((entry = templateZipIs.getNextEntry()) != null)
				{
					if ("xl/vbaProject.bin".equals(entry.getName()))
					{
						break;
					}
				}
				
				if (entry != null)
				{
					ExportZipEntry macroEntry = createEntry("xl/vbaProject.bin");
					OutputStream entryOs = macroEntry.getOutputStream();

					long entryLength = entry.getSize();
					
					byte[] bytes = new byte[10000];
					int ln = 0;
					long readBytesLength = 0;
					while (readBytesLength < entryLength && (ln = templateZipIs.read(bytes)) >= 0)
					{
						readBytesLength += ln;
						entryOs.write(bytes, 0, ln);
					}
				}
			}
		}
		catch (JRException | IOException e)
		{
			throw new JRRuntimeException(e);
		}
		finally
		{
			if (templateZipIs != null)
			{
				try
				{
					templateZipIs.close();
				}
				catch (IOException e)
				{
				}
			}

			if (templateIs != null)
			{
				try
				{
					templateIs.close();
				}
				catch (IOException e)
				{
				}
			}
		}
	}
	
}
