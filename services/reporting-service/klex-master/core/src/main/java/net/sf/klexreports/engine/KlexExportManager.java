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
package net.sf.klexreports.engine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.sf.klexreports.engine.export.HtmlExporter;
import net.sf.klexreports.engine.export.JRXmlExporter;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.Exporter;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleHtmlExporterOutput;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.export.SimpleXmlExporterOutput;


/**
 * Facade class for exporting generated reports into more popular
 * formats such as PDF, HTML and XML.
 * <p>
 * This class contains convenience methods for exporting to only these 3 formats.
 * These methods can process data that comes from different
 * sources and goes to different destinations (files, input and output streams, etc.).
 * </p><p>
 * For exporting to XLS and CSV format or for using special exporter parameters, 
 * the specific exporter class should be used directly.  
 * 
 * @see net.sf.klexreports.engine.KlexPrint
 * @see net.sf.klexreports.engine.export.HtmlExporter
 * @see net.sf.klexreports.pdf.JRPdfExporter
 * @see net.sf.klexreports.engine.export.JRXmlExporter
 * @see net.sf.klexreports.engine.export.ooxml.JRXlsxExporter
 * @see net.sf.klexreports.engine.export.JRCsvExporter
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class KlexExportManager
{
	private static final String EXCEPTION_MESSAGE_KEY_MISSING_EXTENSION_PDF = "extensions.missing.extension.pdf";
	
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private KlexExportManager(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	private static KlexExportManager getDefaultInstance()
	{
		return new KlexExportManager(DefaultKlexReportsContext.getInstance());
	}
	
	
	/**
	 *
	 */
	public static KlexExportManager getInstance(KlexReportsContext klexReportsContext)
	{
		return new KlexExportManager(klexReportsContext);
	}
	
	
	/**
	 * Exports the generated report file specified by the parameter into PDF format.
	 * The resulting PDF file has the same name as the report object inside the source file,
	 * plus the <code>*.pdf</code> extension and it is located in the same directory as the source file.
	 *  
	 * @param sourceFileName source file containing the generated report
	 * @return resulting PDF file name
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public String exportToPdfFile(String sourceFileName) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/* We need the report name. */
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".pdf");
		String destFileName = destFile.toString();
		
		exportToPdfFile(klexPrint, destFileName);
		
		return destFileName;
	}


	/**
	 * Exports the generated report file specified by the first parameter into PDF format,
	 * the result being placed in the second file parameter.
	 *  
	 * @param sourceFileName source file containing the generated report
	 * @param destFileName   file name to place the PDF content into
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToPdfFile(
		String sourceFileName, 
		String destFileName
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(sourceFileName);

		exportToPdfFile(klexPrint, destFileName);
	}

	
	/**
	 * Exports the generated report file specified by the first parameter into PDF format,
	 * the result being placed in the second file parameter.
	 *
	 * @param klexPrint  report object to export 
	 * @param destFileName file name to place the PDF content into
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToPdfFile(
		KlexPrint klexPrint, 
		String destFileName
		) throws JRException
	{
		/*   */
		Exporter exporter = getPdfExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFileName));
		
		exporter.exportReport();
	}


	/**
	 * Exports the generated report read from the supplied input stream into PDF format and
	 * writes the results to the output stream specified by the second parameter.
	 *
	 * @param inputStream  input stream to read the generated report object from
	 * @param outputStream output stream to write the resulting PDF content to
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(inputStream);

		exportToPdfStream(klexPrint, outputStream);
	}

	
	/**
	 * Exports the generated report object received as first parameter into PDF format and
	 * writes the results to the output stream specified by the second parameter.
	 * 
	 * @param klexPrint  report object to export 
	 * @param outputStream output stream to write the resulting PDF content to
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToPdfStream(
		KlexPrint klexPrint, 
		OutputStream outputStream
		) throws JRException
	{
		Exporter exporter = getPdfExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
		
		exporter.exportReport();
	}


	/**
	 * Exports the generated report object received as parameter into PDF format and
	 * returns the binary content as a byte array.
	 * 
	 * @param klexPrint report object to export 
	 * @return byte array representing the resulting PDF content 
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public byte[] exportToPdf(KlexPrint klexPrint) throws JRException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		Exporter exporter = getPdfExporter();
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
		
		exporter.exportReport();
		
		return baos.toByteArray();
	}

	
	/**
	 *
	 */
	private Exporter getPdfExporter()
	{
		try
		{
			Class clazz  = Class.forName("net.sf.klexreports.pdf.JRPdfExporter");
			Constructor constructor = clazz.getConstructor(KlexReportsContext.class);
			return (Exporter)constructor.newInstance(klexReportsContext);
		}
		catch (ClassNotFoundException e)
		{
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_MISSING_EXTENSION_PDF,  
					(Object[])null 
					);
		}
		catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	
	/**
	 * Exports the generated report file specified by the parameter into XML format.
	 * The resulting XML file has the same name as the report object inside the source file,
	 * plus the <code>*.jrpxml</code> extension and it is located in the same directory as the source file.
	 * <p>
	 * When exporting to XML format, the images can be either embedded in the XML content
	 * itself using the Base64 encoder or be referenced as external resources.
	 * If not embedded, the images are placed as distinct files inside a directory
	 * having the same name as the XML destination file, plus the "_files" suffix. 
	 * 
	 * @param sourceFileName    source file containing the generated report
	 * @param isEmbeddingImages flag that indicates whether the images should be embedded in the
	 *                          XML content itself using the Base64 encoder or be referenced as external resources
	 * @return XML representation of the generated report
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public String exportToXmlFile(
		String sourceFileName, 
		boolean isEmbeddingImages
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/* We need the report name. */
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".jrpxml");
		String destFileName = destFile.toString();
		
		exportToXmlFile(
			klexPrint, 
			destFileName,
			isEmbeddingImages
			);
		
		return destFileName;
	}


	/**
	 * Exports the generated report file specified by the first parameter into XML format,
	 * placing the result into the second file parameter.
	 * <p>
	 * If not embedded into the XML content itself using the Base64 encoder, 
	 * the images are placed as distinct files inside a directory having the same name 
	 * as the XML destination file, plus the "_files" suffix. 
	 * 
	 * @param sourceFileName    source file containing the generated report
	 * @param destFileName      file name to place the XML representation into
	 * @param isEmbeddingImages flag that indicates whether the images should be embedded in the
	 *                          XML content itself using the Base64 encoder or be referenced as external resources
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToXmlFile(
		String sourceFileName, 
		String destFileName,
		boolean isEmbeddingImages
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(sourceFileName);

		exportToXmlFile(
			klexPrint, 
			destFileName,
			isEmbeddingImages
			);
	}

	
	/**
	 * Exports the generated report object received as parameter into XML format,
	 * placing the result into the second file parameter.
	 * <p>
	 * If not embedded into the XML content itself using the Base64 encoder, 
	 * the images are placed as distinct files inside a directory having the same name 
	 * as the XML destination file, plus the "_files" suffix. 
	 * 
	 * @param klexPrint       report object to export
	 * @param destFileName      file name to place the XML representation into
	 * @param isEmbeddingImages flag that indicates whether the images should be embedded in the
	 *                          XML content itself using the Base64 encoder or be referenced as external resources
	 *  
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToXmlFile(
		KlexPrint klexPrint, 
		String destFileName,
		boolean isEmbeddingImages
		) throws JRException
	{
		JRXmlExporter exporter = new JRXmlExporter(klexReportsContext);
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		
		SimpleXmlExporterOutput xmlOutput = new SimpleXmlExporterOutput(destFileName);
		xmlOutput.setEmbeddingImages(isEmbeddingImages);
		exporter.setExporterOutput(xmlOutput);
		
		exporter.exportReport();
	}


	/**
	 * Exports the generated report object read from the supplied input stream into XML format,
	 * and writes the result to the output stream specified by the second parameter.
	 * The images are embedded into the XML content itself using the Base64 encoder. 
	 * 
	 * @param inputStream  input stream to read the generated report object from
	 * @param outputStream output stream to write the resulting XML representation to
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToXmlStream(
		InputStream inputStream, 
		OutputStream outputStream
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(inputStream);

		exportToXmlStream(klexPrint, outputStream);
	}

	
	/**
	 * Exports the generated report object supplied as the first parameter into XML format,
	 * and writes the result to the output stream specified by the second parameter.
	 * The images are embedded into the XML content itself using the Base64 encoder. 
	 * 
	 * @param klexPrint  report object to export
	 * @param outputStream output stream to write the resulting XML representation to
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToXmlStream(
		KlexPrint klexPrint, 
		OutputStream outputStream
		) throws JRException
	{
		JRXmlExporter exporter = new JRXmlExporter(klexReportsContext);
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleXmlExporterOutput(outputStream));
		
		exporter.exportReport();
	}


	/**
	 * Exports the generated report object supplied as parameter into XML format
	 * and returs the result as String.
	 * The images are embedded into the XML content itself using the Base64 encoder. 
	 * 
	 * @param klexPrint report object to export
	 * @return XML representation of the generated report
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public String exportToXml(KlexPrint klexPrint) throws JRException
	{
		StringBuilder sb = new StringBuilder();

		JRXmlExporter exporter = new JRXmlExporter(klexReportsContext);
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleXmlExporterOutput(sb));
		
		exporter.exportReport();
		
		return sb.toString();
	}


	/**
	 * Exports the generated report file specified by the parameter into HTML format.
	 * The resulting HTML file has the same name as the report object inside the source file,
	 * plus the <code>*.html</code> extension and it is located in the same directory as the source file.
	 * The images are placed as distinct files inside a directory having the same name 
	 * as the HTML destination file, plus the "_files" suffix. 
	 * 
	 * @param sourceFileName source file containing the generated report
	 * @return resulting HTML file name
	 * @see net.sf.klexreports.engine.export.HtmlExporter
	 */
	public String exportToHtmlFile(
		String sourceFileName
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/* We need the report name. */
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexPrint.getName() + ".html");
		String destFileName = destFile.toString();
		
		exportToHtmlFile(
			klexPrint, 
			destFileName
			);
		
		return destFileName;
	}


	/**
	 * Exports the generated report file specified by the first parameter into HTML format,
	 * placing the result into the second file parameter.
	 * <p>
	 * The images are placed as distinct files inside a directory having the same name 
	 * as the HTML destination file, plus the "_files" suffix. 
	 * 
	 * @param sourceFileName source file containing the generated report
	 * @param destFileName   file name to place the HTML content into
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToHtmlFile(
		String sourceFileName, 
		String destFileName
		) throws JRException
	{
		KlexPrint klexPrint = (KlexPrint)JRLoader.loadObjectFromFile(sourceFileName);

		exportToHtmlFile(
			klexPrint, 
			destFileName
			);
	}

	
	/**
	 * Exports the generated report object received as parameter into HTML format,
	 * placing the result into the second file parameter.
	 * <p>
	 * The images are placed as distinct files inside a directory having the same name 
	 * as the HTML destination file, plus the "_files" suffix. 
	 * 
	 * @param klexPrint  report object to export
	 * @param destFileName file name to place the HTML content into
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 */
	public void exportToHtmlFile(
		KlexPrint klexPrint, 
		String destFileName
		) throws JRException
	{
		HtmlExporter exporter = new HtmlExporter(klexReportsContext);
		
		exporter.setExporterInput(new SimpleExporterInput(klexPrint));
		exporter.setExporterOutput(new SimpleHtmlExporterOutput(destFileName));
		
		exporter.exportReport();
	}
	
	
	/**
	 * @see #exportToPdfFile(String)
	 */
	public static String exportReportToPdfFile(String sourceFileName) throws JRException
	{
		return getDefaultInstance().exportToPdfFile(sourceFileName);
	}


	/**
	 * @see #exportToPdfFile(String, String)
	 */
	public static void exportReportToPdfFile(
		String sourceFileName, 
		String destFileName
		) throws JRException
	{
		getDefaultInstance().exportToPdfFile(sourceFileName, destFileName);
	}

	
	/**
	 * @see #exportToPdfFile(KlexPrint, String)
	 */
	public static void exportReportToPdfFile(
		KlexPrint klexPrint, 
		String destFileName
		) throws JRException
	{
		getDefaultInstance().exportToPdfFile(klexPrint, destFileName);
	}


	/**
	 * @see #exportToPdfStream(InputStream, OutputStream)
	 */
	public static void exportReportToPdfStream(
		InputStream inputStream, 
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().exportToPdfStream(inputStream, outputStream);
	}

	
	/**
	 * Exports the generated report object received as first parameter into PDF format and
	 * writes the results to the output stream specified by the second parameter.
	 * 
	 * @param klexPrint  report object to export 
	 * @param outputStream output stream to write the resulting PDF content to
	 * @see net.sf.klexreports.pdf.JRPdfExporter
	 * @see #exportToPdfStream(KlexPrint, OutputStream)
	 */
	public static void exportReportToPdfStream(
		KlexPrint klexPrint, 
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().exportToPdfStream(klexPrint, outputStream);
	}


	/**
	 * @see #exportToPdf(KlexPrint)
	 */
	public static byte[] exportReportToPdf(KlexPrint klexPrint) throws JRException
	{
		return getDefaultInstance().exportToPdf(klexPrint);
	}

	
	/**
	 * @see #exportToXmlFile(String, String, boolean)
	 */
	public static String exportReportToXmlFile(
		String sourceFileName, 
		boolean isEmbeddingImages
		) throws JRException
	{
		return getDefaultInstance().exportToXmlFile(sourceFileName, isEmbeddingImages);
	}


	/**
	 * @see #exportToXmlFile(String, String, boolean)
	 */
	public static void exportReportToXmlFile(
		String sourceFileName, 
		String destFileName,
		boolean isEmbeddingImages
		) throws JRException
	{
		getDefaultInstance().exportToXmlFile(sourceFileName, destFileName, isEmbeddingImages);
	}

	
	/**
	 * @see #exportToXmlFile(KlexPrint, String, boolean)
	 */
	public static void exportReportToXmlFile(
		KlexPrint klexPrint, 
		String destFileName,
		boolean isEmbeddingImages
		) throws JRException
	{
		getDefaultInstance().exportToXmlFile(klexPrint, destFileName, isEmbeddingImages);
	}


	/**
	 * @see #exportToXmlStream(InputStream, OutputStream)
	 */
	public static void exportReportToXmlStream(
		InputStream inputStream, 
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().exportToXmlStream(inputStream, outputStream);
	}

	
	/**
	 * @see #exportToXmlStream(KlexPrint, OutputStream)
	 */
	public static void exportReportToXmlStream(
		KlexPrint klexPrint, 
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().exportToXmlStream(klexPrint, outputStream);
	}


	/**
	 * @see #exportToXml(KlexPrint)
	 */
	public static String exportReportToXml(KlexPrint klexPrint) throws JRException
	{
		return getDefaultInstance().exportToXml(klexPrint);
	}


	/**
	 * @see #exportToHtmlFile(String)
	 */
	public static String exportReportToHtmlFile(
		String sourceFileName
		) throws JRException
	{
		return getDefaultInstance().exportToHtmlFile(sourceFileName);
	}


	/**
	 * @see #exportToHtmlFile(String, String)
	 */
	public static void exportReportToHtmlFile(
		String sourceFileName, 
		String destFileName
		) throws JRException
	{
		getDefaultInstance().exportToHtmlFile(sourceFileName, destFileName);
	}

	
	/**
	 * @see #exportToHtmlFile(KlexPrint, String)
	 */
	public static void exportReportToHtmlFile(
		KlexPrint klexPrint, 
		String destFileName
		) throws JRException
	{
		getDefaultInstance().exportToHtmlFile(klexPrint, destFileName);
	}
}
