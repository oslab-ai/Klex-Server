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

import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.type.RunDirectionEnum;
import net.sf.klexreports.pdf.common.PdfPhrase;
import net.sf.klexreports.pdf.common.PdfTextAlignment;
import net.sf.klexreports.pdf.common.TextDirection;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PdfTextRenderer extends AbstractPdfTextRenderer
{
	/**
	 * 
	 */
	public PdfTextRenderer(
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
	public void draw()
	{
		TabSegment segment = segments.get(segmentIndex);
		
		float advance = segment.layout.getVisibleAdvance();//getAdvance();
		
		if (bulletChunk != null)
		{
			PdfPhrase phrase = pdfProducer.createPhrase();
			pdfExporter.getPhrase(bulletChunk, bulletText, text, phrase);

			phrase.go(
				- htmlListIndent - 10 + x + drawPosX + leftOffsetFactor * advance,// + leftPadding
				pdfExporter.getCurrentPageFormat().getPageHeight()
					- y
					- topPadding
					- verticalAlignOffset
					//- text.getLeadingOffset()
					+ lineHeight
					- drawPosY,
				- 10 + x + drawPosX + leftOffsetFactor * advance,// + leftPadding
				pdfExporter.getCurrentPageFormat().getPageHeight()
					- y
					- topPadding
					- verticalAlignOffset
					//- text.getLeadingOffset()
					-400//+ lineHeight//FIXMETAB
					- drawPosY,
				lineHeight,
				0,
				PdfTextAlignment.RIGHT,
				TextDirection.LTR
				);
		}

		PdfPhrase phrase = pdfProducer.createPhrase();
		pdfExporter.getPhrase(segment.as, segment.text, text, phrase);
		
		phrase.go(
			x + drawPosX + leftOffsetFactor * advance,// + leftPadding
			pdfExporter.getCurrentPageFormat().getPageHeight()
				- y
				- topPadding
				- verticalAlignOffset
				//- text.getLeadingOffset()
				+ lineHeight
				- drawPosY,
			x + drawPosX + advance + rightOffsetFactor * advance,// + leftPadding
			pdfExporter.getCurrentPageFormat().getPageHeight()
				- y
				- topPadding
				- verticalAlignOffset
				//- text.getLeadingOffset()
				-400//+ lineHeight//FIXMETAB
				- drawPosY,
			lineHeight,//text.getLineSpacingFactor(),// * text.getFont().getSize(),
			0,
			horizontalAlignment == PdfTextAlignment.JUSTIFIED && (!segment.isLastLine || justifyLastLine) 
				? PdfTextAlignment.JUSTIFIED_ALL : horizontalAlignment,
			text.getRunDirection() == RunDirectionEnum.LTR
				? TextDirection.LTR : TextDirection.RTL
			);
	}


	@Override
	public boolean addActualText()
	{
		return false;
	}

}
