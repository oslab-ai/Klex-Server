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
package net.sf.klexreports;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRVirtualizer;
import net.sf.klexreports.engine.KlexCompileManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.SimpleKlexReportsContext;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.export.JRXmlExporter;
import net.sf.klexreports.engine.fill.JRAbstractLRUVirtualizer;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.engine.xml.JRXmlLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleXmlExporterOutput;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class Report
{

	private static final Log log = LogFactory.getLog(Report.class);
	
	private String jrxml;
	private String jrpxml;
	private BiConsumer<Report, KlexPrint> printConsumer = Report::checkDigest;
	
	public Report(String basename)
	{
		this(basename + ".jrxml", basename + ".jrpxml");
	}
	
	public Report(String jrxml, String jrpxml)
	{
		this.jrxml = jrxml;
		this.jrpxml = jrpxml;
	}
	
	protected SimpleKlexReportsContext klexReportsContext;
	protected KlexReport report;
	private KlexFillManager fillManager;
	private String referenceJRPXMLDigest;

	public void init()
	{
		klexReportsContext = new SimpleKlexReportsContext();
		
		try
		{
			compileReport();
			readReferenceDigest();
		}
		catch (JRException | IOException | NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public String getJRXML()
	{
		return jrxml;
	}
	
	public void addPrintConsumer(BiConsumer<Report, KlexPrint> printConsumer)
	{
		this.printConsumer = this.printConsumer.andThen(printConsumer);
	}
	
	public KlexReport compileReport() throws JRException, IOException
	{
		InputStream jrxmlInput = JRLoader.getResourceInputStream(jrxml);
		KlexDesign design;
		try
		{
			design = JRXmlLoader.load(jrxmlInput);
		}
		finally
		{
			jrxmlInput.close();
		}
		
		report = KlexCompileManager.compileReport(design);
		
		//TODO do we need this here?
		fillManager = KlexFillManager.getInstance(klexReportsContext);
		
		return report;
	}

	protected void readReferenceDigest() throws JRException, NoSuchAlgorithmException
	{
		byte[] jrpxmlBytes = JRLoader.loadBytesFromResource(jrpxml);
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.update(jrpxmlBytes);
		referenceJRPXMLDigest = toDigestString(digest);
		log.debug("Reference report digest for " + jrpxml + " is " + referenceJRPXMLDigest);
	}
	
	public void runReport(Map<String, Object> params)
	{
		Map<String, Object> reportParams = reportParams(params);
		try
		{
			KlexPrint print = fillManager.fill(report, reportParams);
			reportComplete(reportParams, print);
		}
		catch (JRException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected Map<String, Object> reportParams(Map<String, Object> params)
	{
		if (params == null)
		{
			params = new HashMap<>();
		}
		params.put(JRParameter.REPORT_LOCALE, Locale.US);
		params.put(JRParameter.REPORT_TIME_ZONE, TimeZone.getTimeZone("GMT"));
		return params;
	}

	protected void reportComplete(Map<String, Object> params, KlexPrint print)
	{
		JRVirtualizer virtualizer = (JRVirtualizer) params.get(JRParameter.REPORT_VIRTUALIZER);
		if (virtualizer instanceof JRAbstractLRUVirtualizer)
		{
			((JRAbstractLRUVirtualizer) virtualizer).setReadOnly(true);
		}
		
		assert !print.getPages().isEmpty();
		
		printConsumer.accept(this, print);
		
		if (virtualizer != null)
		{
			virtualizer.cleanup();
		}
	}

	public void checkDigest(KlexPrint print)
	{
		try
		{
			String digestString = xmlDigest(print);
			log.debug("Report " + jrxml + " got " + digestString);
			assert digestString.equals(referenceJRPXMLDigest);
		} 
		catch (NoSuchAlgorithmException | JRException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected String xmlDigest(KlexPrint print) 
			throws NoSuchAlgorithmException, FileNotFoundException, JRException, IOException
	{
		File outputFile = createXmlOutputFile();
		log.debug("XML export output at " + outputFile.getAbsolutePath());
		
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		try (
			DigestOutputStream out = 
				new DigestOutputStream(
					new BufferedOutputStream(new FileOutputStream(outputFile)), 
					digest
					)
			)
		{
			xmlExport(print, out);
		}
		
		return toDigestString(digest);
	}

	protected String toDigestString(MessageDigest digest)
	{
		byte[] digestBytes = digest.digest();
		StringBuilder digestString = new StringBuilder(digestBytes.length * 2);
		for (byte b : digestBytes)
		{
			digestString.append(String.format("%02x", b));
		}
		return digestString.toString();
	}
	
	protected File createXmlOutputFile() throws IOException
	{
		String outputDirPath = System.getProperty("xmlOutputDir");
		File outputFile;
		if (outputDirPath == null)
		{
			outputFile = File.createTempFile("jr_tests_", ".jrpxml");
		}
		else
		{
			File outputDir = new File(outputDirPath);
			if (!outputDir.exists())
			{
				outputDir.mkdirs(); // for some reason, File.createTempFile method below does not create missing parent folders on Windows
			}
			outputFile = File.createTempFile("jr_tests_", ".jrpxml", outputDir);
		}
		outputFile.deleteOnExit();
		return outputFile;
	}

	protected void xmlExport(KlexPrint print, OutputStream out) throws JRException, IOException
	{
		JRXmlExporter exporter = new JRXmlExporter();
		exporter.setExporterInput(new SimpleExporterInput(print));
		SimpleXmlExporterOutput output = new SimpleXmlExporterOutput(out);
		output.setEmbeddingImages(true);
		exporter.setExporterOutput(output);
		exporter.exportReport();
		out.close();
	}

}
