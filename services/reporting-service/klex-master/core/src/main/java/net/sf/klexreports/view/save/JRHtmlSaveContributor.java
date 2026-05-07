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
package net.sf.klexreports.view.save;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.HtmlExporter;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleHtmlExporterOutput;
import net.sf.klexreports.view.JRSaveContributor;
import net.sf.klexreports.view.SaveContributorFactory;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRHtmlSaveContributor extends JRSaveContributor
{

	/**
	 * 
	 */
	private static final String EXTENSION_HTM = ".htm"; 
	private static final String EXTENSION_HTML = ".html"; 

	/**
	 * @see #JRHtmlSaveContributor(KlexReportsContext, Locale, ResourceBundle)
	 * @deprecated To be removed.
	 */
	public JRHtmlSaveContributor(Locale locale, ResourceBundle resBundle)
	{
		super(locale, resBundle);
	}
	
	/**
	 * 
	 */
	public JRHtmlSaveContributor(
		KlexReportsContext klexReportsContext, 
		Locale locale, 
		ResourceBundle resBundle
		)
	{
		super(klexReportsContext, locale, resBundle);
	}
	
	@Override
	public boolean accept(File file)
	{
		if (file.isDirectory())
		{
			return true;
		}
		String name = file.getName().toLowerCase();
		return (name.endsWith(EXTENSION_HTM) || name.endsWith(EXTENSION_HTML));
	}

	@Override
	public String getDescription()
	{
		return getBundleString("file.desc.html");
	}

	@Override
	public void save(KlexPrint klexPrint, File file) throws JRException
	{
		if (
			!file.getName().toLowerCase().endsWith(EXTENSION_HTM)
			&& !file.getName().toLowerCase().endsWith(EXTENSION_HTML)
			)
		{
			file = new File(file.getAbsolutePath() + EXTENSION_HTML);
		}
			
		if (
			!file.exists() ||
			JOptionPane.OK_OPTION == 
				JOptionPane.showConfirmDialog(
					null, 
					MessageFormat.format(
						getBundleString("file.exists"),
						new Object[]{file.getName()}
						), 
					getBundleString("save"), 
					JOptionPane.OK_CANCEL_OPTION
					)
			)
		{
			HtmlExporter exporter = new HtmlExporter(getKlexReportsContext());
			exporter.setExporterInput(new SimpleExporterInput(klexPrint)); 
			exporter.setExporterOutput(new SimpleHtmlExporterOutput(file));
			exporter.exportReport(); 
		}
	}


	public static class Factory implements SaveContributorFactory
	{
		@Override
		public JRSaveContributor create(
			KlexReportsContext klexReportsContext, 
			Locale locale,
			ResourceBundle resBundle) 
		{
			return new JRHtmlSaveContributor(klexReportsContext, locale, resBundle);
		}
	}
}
