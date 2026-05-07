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
 * Mirko Wawrowsky - mawawrosky@users.sourceforge.net
 */
package net.sf.klexreports.engine.export;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRAbstractExporter;
import net.sf.klexreports.engine.JRCommonText;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRGenericElementType;
import net.sf.klexreports.engine.JRPrintPage;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRStyledText;
import net.sf.klexreports.engine.util.JRStyledTextUtil;
import net.sf.klexreports.export.CsvExporterConfiguration;
import net.sf.klexreports.export.CsvReportConfiguration;
import net.sf.klexreports.export.ExporterInputItem;
import net.sf.klexreports.export.WriterExporterOutput;


/**
 * Exports a KlexReports document to CSV format.
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class JRAbstractCsvExporter<RC extends CsvReportConfiguration, C extends CsvExporterConfiguration, E extends JRExporterContext> 
	extends JRAbstractExporter<RC, C, WriterExporterOutput, E>
{
	public static final String BOM_CHARACTER = "\uFEFF";
	public static final String DEFAULT_ENCLOSURE = "\"";
	public static final String ESCAPE_FORMULA_CHARACTERS = "=+-@";
	protected static final String CSV_EXPORTER_PROPERTIES_PREFIX = JRPropertiesUtil.PROPERTY_PREFIX + "export.csv.";

	/**
	 * The exporter key, as used in
	 * {@link GenericElementHandlerEnviroment#getElementHandler(JRGenericElementType, String)}.
	 */
	public static final String CSV_EXPORTER_KEY = JRPropertiesUtil.PROPERTY_PREFIX + "csv";

	protected String fieldDelimiter;
	protected String recordDelimiter;
	protected boolean forceFieldEnclosure;
	protected String quotes;
	protected boolean escapeFormula;

	/**
	 *
	 */
	protected Writer writer;
	
	protected ExporterNature nature;

	protected int pageIndex;

	
	/**
	 * @see #JRAbstractCsvExporter(KlexReportsContext)
	 */
	public JRAbstractCsvExporter()
	{
		this(DefaultKlexReportsContext.getInstance());
	}

	
	/**
	 *
	 */
	public JRAbstractCsvExporter(KlexReportsContext klexReportsContext)
	{
		super(klexReportsContext);
	}

	
	@Override
	public void exportReport() throws JRException
	{
		/*   */
		ensureKlexReportsContext();
		ensureInput();
		
		initExport();
		
		ensureOutput();
		
		writer = getExporterOutput().getWriter();

		try
		{
			exportReportToWriter();
		}
		catch (IOException e)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_OUTPUT_WRITER_ERROR,
					new Object[]{klexPrint.getName()}, 
					e);
		}
		finally
		{
			getExporterOutput().close();
		}
	}

	
	/**
	 *
	 */
	protected void exportReportToWriter() throws JRException, IOException
	{
		CsvExporterConfiguration configuration = getCurrentConfiguration();
		if (configuration.isWriteBOM())
		{
			WriterExporterOutput output = getExporterOutput();
			Charset charset = Charset.forName(output.getEncoding());
			CharsetEncoder charsetEncoder = charset.newEncoder();
			if (charsetEncoder.canEncode(BOM_CHARACTER))
			{
				writer.write(BOM_CHARACTER);
			}
		}

		List<ExporterInputItem> items = exporterInput.getItems();
		
		for(int reportIndex = 0; reportIndex < items.size(); reportIndex++)
		{
			ExporterInputItem item = items.get(reportIndex);

			setCurrentExporterInputItem(item);

			List<JRPrintPage> pages = klexPrint.getPages();
			if (pages != null && pages.size() > 0)
			{
				PageRange pageRange = getPageRange();
				int startPageIndex = (pageRange == null || pageRange.getStartPageIndex() == null) ? 0 : pageRange.getStartPageIndex();
				int endPageIndex = (pageRange == null || pageRange.getEndPageIndex() == null) ? (pages.size() - 1) : pageRange.getEndPageIndex();

				for(pageIndex = startPageIndex; pageIndex <= endPageIndex; pageIndex++)
				{
					checkInterrupted();
				
					JRPrintPage page = pages.get(pageIndex);

					/*   */
					exportPage(page);
				}
			}
		}
				
		writer.flush();
	}


	/**
	 *
	 */
	protected abstract void exportPage(JRPrintPage page) throws IOException;
	
	
	@Override
	public JRStyledText getStyledText(JRPrintText textElement)
	{
		JRStyledText styledText = textElement.getFullStyledText(noneSelector);
		
		if (styledText != null && !JRCommonText.MARKUP_NONE.equals(textElement.getMarkup()))
		{
			styledText = JRStyledTextUtil.getBulletedText(styledText);
		}

		return styledText;
	}


	/**
	 *
	 */
	protected String prepareText(String source)
	{
		String str = null;
		
		if (source != null)
		{
			boolean putQuotes = 
				forceFieldEnclosure
				|| source.indexOf(fieldDelimiter) >= 0
				|| source.indexOf(recordDelimiter) >= 0;
			
			StringBuilder sb = new StringBuilder();
			StringTokenizer tkzer = new StringTokenizer(source, quotes+"\n", true);
			String token = null;
			while(tkzer.hasMoreTokens())
			{
				token = tkzer.nextToken();
				if (quotes.equals(token))
				{
					putQuotes = true;
					sb.append(quotes+quotes);
				}
				else if ("\n".equals(token))
				{
					//sbuffer.append(" ");
					putQuotes = true;
					sb.append("\n");
				}
				else
				{
					sb.append(token);
				}
			}
			
			str = sb.toString();
			
			if (escapeFormula && !str.isEmpty() && ESCAPE_FORMULA_CHARACTERS.indexOf(str.charAt(0)) >= 0)
			{
				str = " " + str;
			}
			
			if (putQuotes)
			{
				str = quotes + str + quotes;
			}
		}
		return str;
	}
	
	
	@Override
	protected void initExport()
	{
		super.initExport();

		CsvExporterConfiguration configuration = getCurrentConfiguration();
		fieldDelimiter = configuration.getFieldDelimiter();
		recordDelimiter = configuration.getRecordDelimiter();
		forceFieldEnclosure = configuration.getForceFieldEnclosure();
		
		// single character used for field enclosure; white spaces are not considered; default value is "
		quotes = configuration.getFieldEnclosure().trim().length() == 0 
				? DEFAULT_ENCLOSURE 
				: configuration.getFieldEnclosure().trim().substring(0, 1);

		escapeFormula = configuration.getEscapeFormula();
	}
	
	
	@Override
	protected void initReport()
	{
		super.initReport();

		nature = new JRCsvExporterNature(klexReportsContext, filter);
	}


	@Override
	public String getExporterKey()
	{
		return CSV_EXPORTER_KEY;
	}

	
	@Override
	public String getExporterPropertiesPrefix()
	{
		return CSV_EXPORTER_PROPERTIES_PREFIX;
	}
}
