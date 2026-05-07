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

import java.util.List;
import java.util.Map;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JROrigin;
import net.sf.klexreports.engine.JRPrintAnchorIndex;
import net.sf.klexreports.engine.JRPrintPage;
import net.sf.klexreports.engine.JRPropertiesHolder;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRStyle;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.PrintBookmark;
import net.sf.klexreports.engine.PrintPart;
import net.sf.klexreports.engine.PrintParts;
import net.sf.klexreports.engine.type.OrientationEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ReadOnlyPartKlexPrint extends KlexPrint
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private KlexPrint parentKlexPrint;
	private PrintPart part;
	private List<JRPrintPage> pages;
	
	public ReadOnlyPartKlexPrint(KlexPrint klexPrint, PrintPart part, int startPageIndex, int endPageIndex)
	{
		this.parentKlexPrint = klexPrint;
		this.part = part;
		this.pages = klexPrint.getPages().subList(startPageIndex, endPageIndex);
	}

	@Override
	public String getName()
	{
		return part == null ? parentKlexPrint.getName() : part.getName();
	}
		
	@Override
	public void setName(String name)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPageWidth()
	{
		return part == null ? parentKlexPrint.getPageWidth() : part.getPageFormat().getPageWidth();
	}
		
	@Override
	public void setPageWidth(int pageWidth)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPageHeight()
	{
		return part == null ? parentKlexPrint.getPageHeight() : part.getPageFormat().getPageHeight();
	}
		
	@Override
	public void setPageHeight(int pageHeight)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getTopMargin()
	{
		return part == null ? parentKlexPrint.getTopMargin() : part.getPageFormat().getTopMargin();
	}
		
	@Override
	public void setTopMargin(Integer topMargin)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getLeftMargin()
	{
		return part == null ? parentKlexPrint.getLeftMargin() : part.getPageFormat().getLeftMargin();
	}
		
	@Override
	public void setLeftMargin(Integer leftMargin)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getBottomMargin()
	{
		return part == null ? parentKlexPrint.getBottomMargin() : part.getPageFormat().getBottomMargin();
	}
		
	@Override
	public void setBottomMargin(Integer bottomMargin)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getRightMargin()
	{
		return part == null ? parentKlexPrint.getRightMargin() : part.getPageFormat().getRightMargin();
	}
		
	@Override
	public void setRightMargin(Integer rightMargin)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public OrientationEnum getOrientation()
	{
		return part == null ? parentKlexPrint.getOrientation() : part.getPageFormat().getOrientation();
	}
		
	@Override
	public void setOrientation(OrientationEnum orientation)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasProperties()
	{
		return parentKlexPrint.hasProperties();
	}
	
	@Override
	public JRPropertiesMap getPropertiesMap()
	{
		return parentKlexPrint.getPropertiesMap();
	}

	@Override
	public JRPropertiesHolder getParentProperties()
	{
		return null;
	}
	
	@Override
	public String[] getPropertyNames()
	{
		return parentKlexPrint.getPropertyNames();
	}

	@Override
	public String getProperty(String propName)
	{
		return parentKlexPrint.getProperty(propName);
	}

	@Override
	public void setProperty(String propName, String value)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeProperty(String propName)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JRStyle getDefaultStyle()
	{
		return parentKlexPrint.getDefaultStyle();
	}

	@Override
	public synchronized void setDefaultStyle(JRStyle style)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JRDefaultStyleProvider getDefaultStyleProvider()
	{
		return parentKlexPrint.getDefaultStyleProvider();
	}
		
	@Override
	public JRStyle[] getStyles()
	{
		return parentKlexPrint.getStyles();
	}

	@Override
	public List<JRStyle> getStylesList()
	{
		return parentKlexPrint.getStylesList();
	}

	@Override
	public Map<String, JRStyle> getStylesMap()
	{
		return parentKlexPrint.getStylesMap();
	}

	@Override
	public synchronized void addStyle(JRStyle style) throws JRException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void addStyle(JRStyle style, boolean isIgnoreDuplicate) throws JRException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JRStyle removeStyle(String styleName)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JRStyle removeStyle(JRStyle style)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JROrigin[] getOrigins()
	{
		return parentKlexPrint.getOrigins();
	}

	@Override
	public List<JROrigin> getOriginsList()
	{
		return parentKlexPrint.getOriginsList();
	}

	@Override
	public Map<JROrigin, Integer> getOriginsMap()
	{
		return parentKlexPrint.getOriginsMap();
	}

	@Override
	public synchronized void addOrigin(JROrigin origin)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JROrigin removeOrigin(JROrigin origin)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PrintParts getParts()
	{
		return null;
	}

	@Override
	public synchronized void addPart(int pageIndex, PrintPart part)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized PrintPart removePart(int pageIndex)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<JRPrintPage> getPages()
	{
		return pages;
	}

	@Override
	public synchronized void addPage(JRPrintPage page)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void addPage(int index, JRPrintPage page)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JRPrintPage removePage(int index)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<PrintBookmark> getBookmarks()
	{
		return parentKlexPrint.getBookmarks();
	}

	@Override
	public synchronized void addBookmark(PrintBookmark bookmark)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setBookmarks(List<PrintBookmark> bookmarks)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized Map<String,JRPrintAnchorIndex> getAnchorIndexes()
	{
		return parentKlexPrint.getAnchorIndexes();
	}

	@Override
	public String getFormatFactoryClass()
	{
		return parentKlexPrint.getFormatFactoryClass();
	}

	@Override
	public void setFormatFactoryClass(String formatFactoryClass)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLocaleCode()
	{
		return parentKlexPrint.getLocaleCode();
	}

	@Override
	public void setLocaleCode(String localeCode)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTimeZoneId()
	{
		return parentKlexPrint.getTimeZoneId();
	}

	@Override
	public void setTimeZoneId(String timeZoneId)
	{
		throw new UnsupportedOperationException();
	}
}