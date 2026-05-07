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
package net.sf.klexreports.pdf;

import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.type.RunDirectionEnum;
import net.sf.klexreports.engine.util.JRStyledText;
import net.sf.klexreports.pdf.common.PdfPhrase;
import net.sf.klexreports.pdf.common.PdfProducer;
import net.sf.klexreports.pdf.common.PdfTextAlignment;
import net.sf.klexreports.pdf.common.PdfTextRendererContext;
import net.sf.klexreports.pdf.common.TextDirection;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class SimplePdfTextRenderer extends AbstractPdfTextRenderer
{
	private float yLine = 0;
	/**
	 * @deprecated To be removed.
	 */
	protected boolean legacyTextMeasuringFix;
	
	/**
	 * 
	 */
	public SimplePdfTextRenderer(
		KlexReportsContext klexReportsContext, 
		PdfTextRendererContext context
		)
	{
		super(
			klexReportsContext, 
			context.getAwtIgnoreMissingFont(),
			context.getIndentFirstLine(),
			context.getJustifyLastLine()
			);
		
		this.legacyTextMeasuringFix = context.getLegacyTextMeasuringFix();
	}

	
	/**
	 * @deprecated Replaced by {@link #SimplePdfTextRenderer(KlexReportsContext, PdfTextRendererContext)}.
	 */
	public SimplePdfTextRenderer(
		KlexReportsContext klexReportsContext, 
		boolean ignoreMissingFont,
		boolean defaultIndentFirstLine,
		boolean defaultJustifyLastLine
		)
	{
		super(
			klexReportsContext, 
			ignoreMissingFont,
			defaultIndentFirstLine,
			defaultJustifyLastLine
			);
	}

	
	@Override
	public void initialize(
		JRPdfExporter pdfExporter, 
		PdfProducer pdfProducer,
		JRPdfExporterTagHelper tagHelper,
		JRPrintText text, 
		JRStyledText styledText, 
		int offsetX,
		int offsetY
		)
	{
		super.initialize(
			pdfExporter, 
			pdfProducer,
			tagHelper,
			text, 
			styledText, 
			offsetX,
			offsetY
			);
		
		yLine = 
			pdfExporter.getCurrentPageFormat().getPageHeight()
				- y
				- topPadding
				- verticalAlignOffset
				- text.getLeadingOffset();
	}

	
	@Override
	public void render()
	{
		super.render();
	}

	
	@Override
	protected void renderParagraph(
		AttributedCharacterIterator allParagraphs,
		int paragraphStart,
		String paragraphText
		)
	{
		tagHelper.startText(text.getLinkType() != null);

		if (bulletChunk != null)
		{
			PdfPhrase phrase = pdfProducer.createPhrase();
			pdfExporter.getPhrase(bulletChunk, bulletText, text, phrase);

			phrase.go(
				x + leftPadding,
				yLine,
				htmlListIndent + x + leftPadding - 10,
				pdfExporter.getCurrentPageFormat().getPageHeight()
					- y
					- height
					+ bottomPadding,
				0,//text.getLineSpacingFactor(),// * text.getFont().getSize(),
				text.getLineSpacingFactor(),
				PdfTextAlignment.RIGHT,
				TextDirection.LTR
				);
		}

		bulletText = null;
		bulletChunk = null;

		AttributedString paragraph = null;
		
		if (paragraphText == null)
		{
			paragraphText = " ";
			paragraph = 
				new AttributedString(
					paragraphText,
					new AttributedString(
						allParagraphs, 
						paragraphStart, 
						paragraphStart + paragraphText.length()
						).getIterator().getAttributes()
					);
		}
		else
		{
			paragraph = 
				new AttributedString(
					allParagraphs, 
					paragraphStart, 
					paragraphStart + paragraphText.length()
					);
		}
		
		PdfPhrase phrase = pdfProducer.createPhrase();
		pdfExporter.getPhrase(paragraph, paragraphText, text, phrase);
		yLine = phrase.go(
			htmlListIndent + x + leftPadding,
			yLine,
			x + width - rightPadding,
			pdfExporter.getCurrentPageFormat().getPageHeight()
				- y
				- height
				+ bottomPadding
				- (legacyTextMeasuringFix ? 1 : 0),
			0,//text.getLineSpacingFactor(),// * text.getFont().getSize(),
			text.getLineSpacingFactor(),
			horizontalAlignment == PdfTextAlignment.JUSTIFIED && (isLastParagraph && justifyLastLine) 
				? PdfTextAlignment.JUSTIFIED_ALL : horizontalAlignment,
			text.getRunDirection() == RunDirectionEnum.LTR
				? TextDirection.LTR : TextDirection.RTL
			);
		
		tagHelper.endText();
	}


	@Override
	protected AttributedString getAttributedString()
	{
		return styledText.getAttributedString();
	}


	@Override
	public void draw()
	{
		//nothing to do
	}


	@Override
	public boolean addActualText()
	{
		return false;
	}
}
