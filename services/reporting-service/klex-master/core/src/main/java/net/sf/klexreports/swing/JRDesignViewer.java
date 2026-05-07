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
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.convert.ReportConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRDesignViewer extends JRViewer
{
	private static final Log log = LogFactory.getLog(JRDesignViewer.class);

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	/**
	 * @see #JRDesignViewer(KlexReportsContext, String, boolean)
	 */
	public JRDesignViewer(String fileName, boolean isXML) throws JRException
	{
		this(DefaultKlexReportsContext.getInstance(), fileName, isXML);
	}
	
	/**
	 * @see #JRDesignViewer(KlexReportsContext, InputStream, boolean)
	 */
	public JRDesignViewer(InputStream is, boolean isXML) throws JRException
	{
		this(DefaultKlexReportsContext.getInstance(), is, isXML);
	}
	
	/**
	 * @see #JRDesignViewer(KlexReportsContext, JRReport)
	 */
	public JRDesignViewer(JRReport report) throws JRException
	{
		this(DefaultKlexReportsContext.getInstance(), report);
	}
	
	/**
	 *
	 */
	public JRDesignViewer(
		KlexReportsContext klexReportsContext,
		String fileName, 
		boolean isXML
		) throws JRException
	{
		super(klexReportsContext, fileName, isXML, null, null);
		hideUnusedComponents();
	}
	
	/**
	 *
	 */
	public JRDesignViewer(
		KlexReportsContext klexReportsContext,
		InputStream is, 
		boolean isXML
		) throws JRException
	{
		super(klexReportsContext, is, isXML, null, null);
		hideUnusedComponents();
	}
	
	/**
	 *
	 */
	public JRDesignViewer(
		KlexReportsContext klexReportsContext,
		JRReport report
		) throws JRException
	{
		super(klexReportsContext, new ReportConverter(klexReportsContext, report, false).getKlexPrint(), null, null);
		//reconfigureReloadButton();
		hideUnusedComponents();
	}
	
	private void hideUnusedComponents()
	{
		pnlStatus.setVisible(false);
	}

	@Override
	protected void initViewerContext(KlexReportsContext klexReportsContext, Locale locale, ResourceBundle resBundle)
	{
		viewerContext = new JRDesignViewerController(klexReportsContext, locale, resBundle);
		setLocale(viewerContext.getLocale());
		viewerContext.addListener(this);
	}

	@Override
	protected JRViewerToolbar createToolbar()
	{
		return new JRDesignViewerToolbar(viewerContext);
	}

	@Override
	protected JRViewerPanel createViewerPanel()
	{
		return new JRDesignViewerPanel(viewerContext);
	}

}
