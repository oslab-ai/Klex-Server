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
package net.sf.klexreports.view;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.filechooser.FileFilter;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class JRSaveContributor extends FileFilter
{
	private KlexReportsContext klexReportsContext;
	private Locale locale;
	private ResourceBundle resourceBundle;
	
	/**
	 * @see #JRSaveContributor(KlexReportsContext, Locale, ResourceBundle)
	 * @deprecated To be removed.
	 */
	public JRSaveContributor()
	{
		this(null, null);
	}
	
	/**
	 * @see #JRSaveContributor(KlexReportsContext, Locale, ResourceBundle)
	 * @deprecated To be removed.
	 */
	public JRSaveContributor(Locale locale, ResourceBundle resBundle)
	{
		this(DefaultKlexReportsContext.getInstance(), locale, resBundle);
	}
	
	/**
	 * 
	 */
	public JRSaveContributor(
		KlexReportsContext klexReportsContext,
		Locale locale, 
		ResourceBundle resBundle
		)
	{
		this.klexReportsContext = klexReportsContext;
		
		if (locale != null)
		{
			this.locale = locale;
		}
		else
		{
			this.locale = Locale.getDefault();
		}

		if (resBundle == null)
		{
			this.resourceBundle = ResourceBundle.getBundle("net/sf/klexreports/view/viewer", this.locale);
		}
		else
		{
			this.resourceBundle = resBundle;
		}
	}
	
	
	/**
	 * 
	 */
	protected KlexReportsContext getKlexReportsContext()
	{
		return klexReportsContext;
	}

	
	/**
	 * 
	 */
	protected String getBundleString(String key)
	{
		return resourceBundle.getString(key);
	}

	
	/**
	 * 
	 */
	public abstract void save(KlexPrint klexPrint, File file) throws JRException;


}
