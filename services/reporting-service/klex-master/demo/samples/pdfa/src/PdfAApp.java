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
import java.io.File;

import net.sf.klexreports.engine.JREmptyDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexFillManager;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.util.AbstractSampleApp;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.export.SimpleExporterInput;
import net.sf.klexreports.export.SimpleOutputStreamExporterOutput;
import net.sf.klexreports.pdf.JRPdfExporter;
import net.sf.klexreports.pdf.SimplePdfExporterConfiguration;
import net.sf.klexreports.pdf.type.PdfVersionEnum;
import net.sf.klexreports.pdf.type.PdfaConformanceEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PdfAApp extends AbstractSampleApp
{


	/**
	 *
	 */
	public static void main(String[] args)
	{
		main(new PdfAApp(), args);
	}
	
	
	@Override
	public void test() throws JRException
	{
		compile();
		fill();
		pdfa();
	}


	/**
	 *
	 */
	public void fill() throws JRException
	{
		long start = System.currentTimeMillis();
		KlexFillManager.fillReportToFile("target/reports/PdfA1Report.klex", null, new JREmptyDataSource());
		KlexFillManager.fillReportToFile("target/reports/PdfA2Report.klex", null, new JREmptyDataSource());
		KlexFillManager.fillReportToFile("target/reports/PdfA3Report.klex", null, new JREmptyDataSource());
		KlexFillManager.fillReportToFile("target/reports/TaggedPdfA1Report.klex", null, new JREmptyDataSource());
		KlexFillManager.fillReportToFile("target/reports/TabularPdfA1Report.klex", null, new JREmptyDataSource(10));
		System.err.println("Filling time : " + (System.currentTimeMillis() - start));
	}


	/**
	 *
	 */
	public void pdfa() throws JRException
	{
		File[] sourceFiles = new File[] {
			new File("target/reports/PdfA1Report.jrprint"),
			new File("target/reports/PdfA2Report.jrprint"),
			new File("target/reports/PdfA3Report.jrprint"),
			new File("target/reports/TaggedPdfA1Report.jrprint"),
			new File("target/reports/TabularPdfA1Report.jrprint")
		};
		
		for (int i = 0; i < sourceFiles.length; ++i) 
		{
			long start = System.currentTimeMillis();
			
			KlexPrint klexPrint = (KlexPrint) JRLoader.loadObject(sourceFiles[i]);
			
			File destFile = new File(sourceFiles[i].getParent(), klexPrint.getName() + ".pdf");
			
			JRPdfExporter exporter = new JRPdfExporter();
	
			exporter.setExporterInput(new SimpleExporterInput(klexPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
			SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
	
			configuration.setTagged(true);
			configuration.setCompressed(false);
			configuration.setMetadataTitle("Some title");
			configuration.setMetadataAuthor("Some author");
			configuration.setMetadataSubject("Some subject");
			configuration.setMetadataKeywords("The first keyword, the second keyword");
			configuration.setMetadataCreator("Some author");
			configuration.setMetadataProducer("Some producer");
			configuration.setEmbedIccProfile(Boolean.TRUE);
			configuration.setIccProfilePath("./sRGB_IEC61966-2-1_no_black_scaling.icc");
			
			switch (i)
			{
				case 1:
					configuration.setPdfaConformance(PdfaConformanceEnum.PDFA_2A);
					configuration.setPdfVersion(PdfVersionEnum.VERSION_1_7);
				break;
				case 2:
					configuration.setPdfaConformance(PdfaConformanceEnum.PDFA_3A);
					configuration.setPdfVersion(PdfVersionEnum.VERSION_1_7);
				break;
				default:
					configuration.setPdfaConformance(PdfaConformanceEnum.PDFA_1A);
					configuration.setPdfVersion(PdfVersionEnum.VERSION_1_4);
				break;
			}
	
			exporter.setConfiguration(configuration);
			exporter.exportReport();
	
			System.err.println("PDF/A creation time : " + (System.currentTimeMillis() - start));
		}
	}


}
