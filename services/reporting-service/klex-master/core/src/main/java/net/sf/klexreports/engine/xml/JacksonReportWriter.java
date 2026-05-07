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
package net.sf.klexreports.engine.xml;

import java.io.IOException;
import java.io.Writer;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRReport;
import net.sf.klexreports.engine.JRTemplate;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.VersionComparator;
import net.sf.klexreports.jackson.util.JacksonUtil;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JacksonReportWriter implements ReportWriter
{

	private final KlexReportsContext klexReportsContext;
	
	public JacksonReportWriter(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}

	@Override
	public boolean writeReport(JRReport report, String encoding, Writer out)
			throws IOException
	{
		String version = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(report, JRXmlWriter.PROPERTY_REPORT_VERSION);
		if (version == null || new VersionComparator().compare(version, JRConstants.VERSION_7_0_0) >= 0)
		{
			JacksonUtil.getInstance(klexReportsContext).writeXml(report, out);
			return true;
		}
		return false;
	}

	@Override
	public boolean writeTemplate(JRTemplate template, String encoding,
			Writer out) throws IOException
	{
		String version = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(JRXmlWriter.PROPERTY_REPORT_VERSION);
		if (version == null || new VersionComparator().compare(version, JRConstants.VERSION_7_0_0) >= 0)
		{
			JacksonUtil.getInstance(klexReportsContext).writeXml(template, out);
			return true;
		}
		return false;
	}

}
