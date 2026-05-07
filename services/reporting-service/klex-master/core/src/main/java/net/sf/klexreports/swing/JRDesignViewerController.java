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
package net.sf.klexreports.swing;

import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.convert.ReportConverter;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.engine.xml.JRXmlLoader;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRDesignViewerController extends JRViewerController
{
	/**
	 * @see #JRDesignViewerController(KlexReportsContext, Locale, ResourceBundle)
	 */
	public JRDesignViewerController(Locale locale, ResourceBundle resBundle)
	{
		this(DefaultKlexReportsContext.getInstance(), locale, resBundle);
	}

	/**
	 * 
	 */
	public JRDesignViewerController(
		KlexReportsContext klexReportsContext,
		Locale locale, 
		ResourceBundle resBundle
		)
	{
		super(klexReportsContext, locale, resBundle);
	}

	@Override
	protected void setReport(String fileName, boolean isXmlReport) throws JRException
	{
		if (isXmlReport)
		{
			KlexDesign klexDesign = JRXmlLoader.load(fileName);
			setReport(klexDesign);
		}
		else
		{
			setReport((JRReport) JRLoader.loadObjectFromFile(fileName));
		}
	}

	@Override
	protected void setReport(InputStream is, boolean isXmlReport) throws JRException
	{
		if (isXmlReport)
		{
			KlexDesign klexDesign = JRXmlLoader.load(is);
			setReport(klexDesign);
		}
		else
		{
			setReport((JRReport) JRLoader.loadObject(is));
		}
	}

	private void setReport(JRReport report) throws JRException
	{
		this.klexPrint = new ReportConverter(getKlexReportsContext(), report, false).getKlexPrint();		
	}
}
