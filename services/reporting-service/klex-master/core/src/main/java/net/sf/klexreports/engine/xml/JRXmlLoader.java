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

/*
 * Contributors:
 * Artur Biesiadowski - abies@users.sourceforge.net 
 */
package net.sf.klexreports.engine.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.JRLoader;


/**
 * Utility class that helps parsing a JRXML file into a 
 * {@link net.sf.klexreports.engine.design.KlexDesign} object.
 * <p>
 * This can be done using one of the <code>load(...)</code> or <code>loadXml</code> 
 * methods published by this class. Applications might need to do this in cases where report
 * templates kept in their source form (JRXML) must be modified at runtime based on
 * some user input and then compiled on the fly for filling with data.
 * </p>
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRXmlLoader
{
	
	public static final String EXCEPTION_MESSAGE_KEY_NO_LOADER = "xml.loader.unknown.subdataset";

	/**
	 *
	 */
	private final KlexReportsContext klexReportsContext;

	private boolean ignoreConsistencyProblems;
		
	/**
	 *
	 */
	public JRXmlLoader(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}

	/**
	 *
	 */
	public KlexReportsContext getKlexReportsContext()
	{
		return klexReportsContext;
	}


	/**
	 * @see #load(KlexReportsContext, String)
	 */
	public static KlexDesign load(String sourceFileName) throws JRException//FIXMEREPO consider renaming
	{
		return load(DefaultKlexReportsContext.getInstance(),  sourceFileName);
	}


	/**
	 *
	 */
	public static KlexDesign load(KlexReportsContext klexReportsContext, String sourceFileName) throws JRException//FIXMEREPO consider renaming
	{
		return load(klexReportsContext, new File(sourceFileName));
	}


	/**
	 * @see #load(KlexReportsContext, File)
	 */
	public static KlexDesign load(File file) throws JRException
	{
		return load(DefaultKlexReportsContext.getInstance(), file);
	}


	/**
	 *
	 */
	public static KlexDesign load(KlexReportsContext klexReportsContext, File file) throws JRException
	{
		KlexDesign klexDesign = null;

		try (FileInputStream fis = new FileInputStream(file))
		{
			klexDesign = JRXmlLoader.load(klexReportsContext, fis);
		}
		catch (IOException e)
		{
			throw new JRException(e);
		}

		return klexDesign;
	}


	/**
	 * @see #load(KlexReportsContext, InputStream)
	 */
	public static KlexDesign load(InputStream is) throws JRException
	{
		return load(DefaultKlexReportsContext.getInstance(), is);
	}


	/**
	 *
	 */
	public static KlexDesign load(KlexReportsContext klexReportsContext, InputStream is) throws JRException
	{
		KlexDesign klexDesign = null;

		JRXmlLoader xmlLoader = new JRXmlLoader(klexReportsContext);
		
		klexDesign = xmlLoader.loadXML(is);

		return klexDesign;
	}


	/**
	 *
	 */
	public KlexDesign loadXML(InputStream is) throws JRException
	{
		byte[] data = JRLoader.loadBytes(is);
		List<ReportLoader> loaders = klexReportsContext.getExtensions(ReportLoader.class);
		for (ReportLoader reportLoader : loaders)
		{
			//TODO legacyxml ignoreConsistencyProblems
			KlexDesign report = reportLoader.loadReport(klexReportsContext, data);
			if (report != null)
			{
				return report;
			}
		}
		//TODO legacyxml 
		throw new JRException("Unable to load report");
	}
	
	/**
	 * Returns true if the loader is set to ignore consistency problems
	 * @return the ignoreConsistencyProblems flag.
	 */
	public boolean isIgnoreConsistencyProblems() {
		return ignoreConsistencyProblems;
	}
	
	/**
	 * Allows to enable or disable the reporting of consistency problems. Consistency 
	 * problems are problems in the logical structure of the report such as references
	 * to missing groups and fonts.
	 * 
	 * @param ignoreConsistencyProblems The ignoreConsistencyProblems value to set.
	 */
	public void setIgnoreConsistencyProblems(boolean ignoreConsistencyProblems) {
		this.ignoreConsistencyProblems = ignoreConsistencyProblems;
	}
}
