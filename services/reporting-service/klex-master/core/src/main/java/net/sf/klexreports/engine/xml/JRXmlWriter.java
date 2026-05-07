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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRReport;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * A writer that produces the JRXML representation of an in-memory report.
 * <p>
 * Sometimes report designs are generated automatically using the KlexReports 
 * API. Report design objects obtained this way can be serialized for disk storage or 
 * transferred over the network, but they also can be stored in JRXML format.
 * </p><p>
 * The JRXML representation of a given report design object can be obtained by using one 
 * of the <code>public static writeReport()</code> methods exposed by this class.
 * </p>
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 * @author Minor enhancements by Barry Klawans (bklawans@users.sourceforge.net)
 */
public class JRXmlWriter
{
	
	/**
	 * Property that specifies the KlexReports version associated with this report. Report elements/attributes newer than 
	 * this version are neglected by the JRXML writers when a report template is generated. If not set, all elements/attributes 
	 * will be printed out. 
	 * 
	 * @see JRXmlWriter
	 */
	@Property(
			category = PropertyConstants.CATEGORY_OTHER,
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT},
			sinceVersion = PropertyConstants.VERSION_4_8_0
			)
	public static final String PROPERTY_REPORT_VERSION = JRPropertiesUtil.PROPERTY_PREFIX + "report.version";

	public static final String EXCEPTION_MESSAGE_KEY_FILE_WRITE_ERROR = "xml.writer.file.write.error";
	public static final String EXCEPTION_MESSAGE_KEY_OUTPUT_STREAM_WRITE_ERROR = "xml.writer.output.stream.write.error";
	public static final String EXCEPTION_MESSAGE_KEY_REPORT_DESIGN_WRITE_ERROR = "xml.writer.report.design.write.error";
	public static final String EXCEPTION_MESSAGE_KEY_UNSUPPORTED_CHART_TYPE = "xml.writer.unsupported.chart.type";
	
	@Property(
			name = "net.sf.klexreports.jrxml.writer.exclude.properties.{suffix}",
			category = PropertyConstants.CATEGORY_OTHER,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_6_1_1
			)
	public static final String PREFIX_EXCLUDE_PROPERTIES = 
			JRPropertiesUtil.PROPERTY_PREFIX + "jrxml.writer.exclude.properties.";

	@Property(
			name = "net.sf.klexreports.jrxml.writer.exclude.uuids",
			category = PropertyConstants.CATEGORY_OTHER,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_6_5_1
			)
	public static final String PROPERTY_EXCLUDE_UUIDS = 
			JRPropertiesUtil.PROPERTY_PREFIX + "jrxml.writer.exclude.uuids";

	/**
	 *
	 */
	private KlexReportsContext klexReportsContext;

	/**
	 *
	 */
	private JRReport report;


	/**
	 *
	 */
	public JRXmlWriter(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}


	/**
	 *
	 */
	public JRReport getReport()
	{
		return report;
	}


	/**
	 *
	 */
	public String write(JRReport report, String encoding)
	{
		StringWriter buffer = new StringWriter();
		try
		{
			writeReport(report, encoding, buffer);
		}
		catch (IOException e)
		{
			// doesn't actually happen
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_REPORT_DESIGN_WRITE_ERROR,
					(Object[])null,
					e);
		}
		return buffer.toString();
	}


	/**
	 *
	 */
	public void write(
		JRReport report,
		String destFileName,
		String encoding
		) throws JRException
	{
		try (
			Writer out = 
				new OutputStreamWriter(
					new BufferedOutputStream(
						new FileOutputStream(destFileName)
						), 
					encoding
					)
			)
		{
			writeReport(report, encoding, out);
		}
		catch (IOException e)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_FILE_WRITE_ERROR,
					new Object[]{destFileName},
					e);
		}
	}


	/**
	 *
	 */
	public void write(
		JRReport report,
		OutputStream outputStream,
		String encoding
		) throws JRException
	{
		try
		{
			Writer out = new OutputStreamWriter(outputStream, encoding);
			writeReport(report, encoding, out);
		}
		catch (Exception e)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_OUTPUT_STREAM_WRITE_ERROR,
					new Object[]{report.getName()},
					e);
		}
	}


	/**
	 * @see #write(JRReport, String)
	 */
	public static String writeReport(JRReport report, String encoding)
	{
		return new JRXmlWriter(DefaultKlexReportsContext.getInstance()).write(report, encoding);
	}


	/**
	 * @see #write(JRReport, String, String)
	 */
	public static void writeReport(
		JRReport report,
		String destFileName,
		String encoding
		) throws JRException
	{
		new JRXmlWriter(DefaultKlexReportsContext.getInstance()).write(report, destFileName, encoding);
	}


	/**
	 * @see #write(JRReport, OutputStream, String)
	 */
	public static void writeReport(
		JRReport report,
		OutputStream outputStream,
		String encoding
		) throws JRException
	{
			new JRXmlWriter(DefaultKlexReportsContext.getInstance()).write(report, outputStream, encoding);
	}


	/**
	 *
	 */
	protected void writeReport(JRReport report, String encoding, Writer out) throws IOException
	{
		List<ReportWriterFactory> writerFactories = klexReportsContext.getExtensions(ReportWriterFactory.class);
		for (ReportWriterFactory writerFactory : writerFactories)
		{
			ReportWriter reportWriter = writerFactory.createReportWriter(klexReportsContext);
			boolean written = reportWriter.writeReport(report, encoding, out);
			if (written)
			{
				return;
			}
		}
		String version = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(JRXmlWriter.PROPERTY_REPORT_VERSION);
		throw new JRRuntimeException("No report writer found for version " + version + ".");
	}
	
	
}
