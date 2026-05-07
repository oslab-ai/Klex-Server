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
package net.sf.klexreports.engine.export;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPrintFrame;
import net.sf.klexreports.engine.JRPrintPage;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;

/**
 * A utility class that applies {@link GenericElementTransformer GenericElementTransformers}
 * to a filled report.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see GenericElementTransformer
 */
public final class GenericElementReportTransformer
{
	private static final Log log = LogFactory.getLog(GenericElementReportTransformer.class);
	
	protected static class TransformerContext implements GenericElementTransformerContext
	{
		private final KlexReportsContext klexReportsContext;
		private final KlexPrint report;

		public TransformerContext(
			KlexReportsContext klexReportsContext,
			KlexPrint report
			)
		{
			this.klexReportsContext = klexReportsContext;
			this.report = report;
		}

		@Override
		public KlexReportsContext getKlexReportsContext()
		{
			return klexReportsContext;
		}

		@Override
		public KlexPrint getReport()
		{
			return report;
		}
	}

	/**
	 * Applies generic element transformers of a specific key to a filled report
	 * by replacing generic elements with the transformed elements.
	 * 
	 * @param report the report
	 * @param transformerExportKey the key used to resolve element transformers
	 * @see GenericElementTransformer
	 * @see GenericElementHandlerBundle#getHandler(String, String)
	 */
	public static void transformGenericElements(
		KlexReportsContext klexReportsContext,
		KlexPrint report, 
		String transformerExportKey
		)
	{
		TransformerContext transformerContext = new TransformerContext(klexReportsContext, report);
		List<JRPrintPage> pages = report.getPages();
		for (Iterator<JRPrintPage> pageIt = pages.iterator(); pageIt.hasNext();)
		{
			JRPrintPage page = pageIt.next();
			transformElements(transformerContext, transformerExportKey, 
					page.getElements());
		}
	}

	protected static void transformElements(
			TransformerContext context, 
			String transformerExportKey, 
			List<JRPrintElement> elements)
	{
		for (ListIterator<JRPrintElement> it = elements.listIterator(); it.hasNext();)
		{
			JRPrintElement element = it.next();
			if (element instanceof JRGenericPrintElement)
			{
				JRGenericPrintElement genericElement = 
					(JRGenericPrintElement) element;
				GenericElementTransformer handler = 
						(GenericElementTransformer) GenericElementHandlerEnviroment.getInstance(context.getKlexReportsContext()).getElementHandler(
						genericElement.getGenericType(), transformerExportKey);
				if (handler != null && handler.toExport(genericElement))
				{
					JRPrintElement transformed = handler.transformElement(
							context, genericElement);
					
					if (log.isDebugEnabled())
					{
						log.debug("Transformed element " + genericElement 
								+ " to " + transformed
								+ " using the " + transformerExportKey + " transformer");
					}
					
					// assuming that the list is modifiable
					it.set(transformed);
				}
			}
			else if (element instanceof JRPrintFrame)
			{
				JRPrintFrame frame = (JRPrintFrame) element;
				transformElements(context, transformerExportKey, frame.getElements());
			}
		}
		
	}
	

	private GenericElementReportTransformer()
	{
	}
}
