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
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.KlexCompileManager;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.SimpleKlexReportsContext;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.engine.util.JRResourcesUtil;
import net.sf.klexreports.engine.xml.JRXmlLoader;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class AbstractTest
{
	private static final Log log = LogFactory.getLog(AbstractTest.class);
	
	private static final String TEST = "TEST";

	private ITestContext testContext;
	private KlexReportsContext klexReportsContext;

	@BeforeClass
	public void init(ITestContext ctx) throws JRException, IOException
	{
		testContext = ctx;
		klexReportsContext = new SimpleKlexReportsContext();
	}

	@Test(dataProvider = "testArgs")
	public void testReport(String folderName, String fileNamePrefix, String referenceFileNamePrefix) 
			throws JRException, NoSuchAlgorithmException, IOException
	{
		runReport(folderName, fileNamePrefix, referenceFileNamePrefix);
	}
	
	protected Object[][] runReportArgs(String folderName, String fileNamePrefix, int maxFileNumber)
	{
		return runReportArgs(folderName, fileNamePrefix, fileNamePrefix, maxFileNumber);
	}

	protected Object[][] runReportArgs(String folderName, String fileNamePrefix, String referenceFileNamePrefix, int maxFileNumber)
	{
		List<Object[]> args = new ArrayList<>(maxFileNumber);
		for (int i = 1; i <= maxFileNumber; i++)
		{
			args.add(
				new Object[] {
					folderName, 
					folderName + "/" + fileNamePrefix + "." + i + ".jrxml", 
					folderName + "/" + referenceFileNamePrefix + "." + i
					}
				);
		}
		return args.toArray(new Object[args.size()][]);
	}

	protected void runReport(String folderName, String jrxmlFileName, String referenceFileNamePrefix) 
			throws JRException, IOException, NoSuchAlgorithmException, FileNotFoundException
	{
		KlexFillManager fillManager = KlexFillManager.getInstance(getKlexReportsContext());
		
		HashMap<String, Object> params = new HashMap<>();
		params.put(JRParameter.REPORT_LOCALE, Locale.US);
		params.put(JRParameter.REPORT_TIME_ZONE, TimeZone.getTimeZone("GMT"));
		params.put(TEST, this);
		
		log.debug("Running report " + jrxmlFileName);
		
		try
		{
			KlexReport report = compileReport(jrxmlFileName);
			if (report != null)
			{
				KlexPrint print = fillManager.fill(report, params);
				
				assert !print.getPages().isEmpty();
				
				String exportDigest = getExportDigest(referenceFileNamePrefix, print);
				log.debug("Plain report got " + exportDigest);
				
				boolean digestMatch = false;

				String referenceExportDigest = getDigestFromFile(referenceFileNamePrefix + "." + getExportFileExtension() + ".sha");
				
				if (exportDigest.equals(referenceExportDigest))
				{
					digestMatch = true;
				}
//				else
//				{
//					//fallback to account for JDK differences
//					referenceExportDigest = getDigestFromFile(referenceFileNamePrefix + ".2." + getExportFileExtension() + ".sha");
//					if (referenceExportDigest != null)
//					{
//						digestMatch = exportDigest.equals(referenceExportDigest);
//					}
//				}
				
				assert digestMatch;
			}
		}
		catch (AssertionError e)
		{
			throw e;
		}
		catch (Throwable t)
		{
			String referenceErrorDigest = getDigestFromFile(referenceFileNamePrefix + ".err.sha");
			if (referenceErrorDigest == null)
			{
				log.error("Report " + jrxmlFileName + " failed", t);
				//we don't have a reference error, it's an unexpected exception
				throw t;
			}

			String errorDigest = errExportDigest(t);

			assert errorDigest.equals(referenceErrorDigest);
		}
	}

	protected KlexReportsContext getKlexReportsContext()
	{
		return klexReportsContext;
	}

	protected void setKlexReportsContext(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}

	/**
	 * This method is used for compiling subreports.
	 */
	public KlexReport compileReport(String jrxmlFileName) throws JRException, IOException
	{
		KlexReport klexReport = null;
		
		InputStream jrxmlInput = JRLoader.getResourceInputStream(jrxmlFileName);

		if (jrxmlInput != null)
		{
			KlexDesign design;
			try
			{
				design = JRXmlLoader.load(jrxmlInput);
			}
			finally
			{
				jrxmlInput.close();
			}
			klexReport = KlexCompileManager.compileReport(design);
		}
		
		return klexReport;
	}

	protected KlexReport compileReport(File jrxmlFile) throws JRException, IOException
	{
		KlexDesign design = JRXmlLoader.load(jrxmlFile);
		
		return KlexCompileManager.compileReport(design);
	}

	protected String getDigestFromFile(String fileName) throws JRException
	{
		URL resourceURL = JRResourcesUtil.findClassLoaderResource(fileName, null);
		if (resourceURL == null)
		{
			log.debug("did not find resource " + fileName);
			return null;
		}
		
		byte[] bytes = JRLoader.loadBytes(resourceURL);
		String digest = null;
		try
		{
			digest = new String(bytes, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new JRException(e);
		}
		log.debug("Reference report digest is " + digest + " for " + fileName);
		return digest;
	}
	
	protected String getExportDigest(String referenceFileNamePrefix, KlexPrint print) 
			throws NoSuchAlgorithmException, FileNotFoundException, JRException, IOException
	{
		File outputFile = new File(new File(testContext.getOutputDirectory()), referenceFileNamePrefix + "." + getExportFileExtension());
		File outputDir = outputFile.getParentFile();
		
		if (!outputDir.exists())
		{
			outputDir.mkdirs();
		}
		
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
			export(print, out);
		}
		
		String digestSha = toDigestString(digest);
		
		File outputShaFile = new File(new File(testContext.getOutputDirectory()), referenceFileNamePrefix + "." + getExportFileExtension() + ".sha");
		OutputStreamWriter shaWriter = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputShaFile)), "UTF-8");

		try
		{
			shaWriter.write(digestSha);
		}
		finally 
		{
			shaWriter.close();
		}
		
		return digestSha;
	}

	protected String errExportDigest(Throwable t) 
			throws NoSuchAlgorithmException, FileNotFoundException, JRException, IOException
	{
		File outputFile = createTmpOutputFile("err");
		log.debug("Error stack trace at " + outputFile.getAbsolutePath());
		
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		try (
			OutputStreamWriter osw = 
				new OutputStreamWriter(
					new DigestOutputStream(
						new BufferedOutputStream(new FileOutputStream(outputFile)), 
						digest
						)
					)
			)
		{
			osw.write(String.valueOf(t.getMessage()));
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
	
	protected File createTmpOutputFile(String fileExtension) throws IOException
	{
		String outputDirPath = System.getProperty("outputDir");
		File outputFile;
		if (outputDirPath == null)
		{
			outputFile = File.createTempFile("jr_tests_", "." + fileExtension);
		}
		else
		{
			File outputDir = new File(outputDirPath);
			if (!outputDir.exists())
			{
				outputDir.mkdirs(); // for some reason, File.createTempFile method below does not create missing parent folders on Windows
			}
			outputFile = File.createTempFile("jr_tests_", "." + fileExtension, outputDir);
		}
		outputFile.deleteOnExit();
		return outputFile;
	}

	protected abstract void export(KlexPrint print, OutputStream out) throws JRException, IOException;

	protected abstract String getExportFileExtension();
}
