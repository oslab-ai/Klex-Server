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
package net.sf.klexreports.engine.export.oasis;

import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRTextAlignment;
import net.sf.klexreports.engine.export.JRExporterGridCell;
import net.sf.klexreports.engine.type.HorizontalTextAlignEnum;
import net.sf.klexreports.engine.type.ModeEnum;
import net.sf.klexreports.engine.type.RotationEnum;
import net.sf.klexreports.engine.type.VerticalTextAlignEnum;
import net.sf.klexreports.engine.util.JRColorUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class CellStyle extends BorderStyle
{
	//private String fill;
	private String backcolor;
	
	private final String horizontalAlignment;
	private final String verticalAlignment;
	private final boolean shrinkToFit;
	private final boolean wrapText;

	
	/**
	 *
	 */
	public CellStyle(WriterHelper styleWriter, JRExporterGridCell gridCell, boolean shrinkToFit, boolean wrapText)
	{
		super(styleWriter);

		JRPrintElement element = gridCell.getElement();
		
		if (element != null && element.getMode() == ModeEnum.OPAQUE)
		{
			//fill = "solid";
			backcolor = JRColorUtil.getColorHexa(element.getBackcolor());
		}
		else
		{
			//fill = "none";
			if (gridCell.getBackcolor() != null)
			{
				backcolor = JRColorUtil.getColorHexa(gridCell.getBackcolor());
			}
		}

		RotationEnum rotation = element instanceof JRPrintText ? ((JRPrintText)element).getRotation() : RotationEnum.NONE;
		VerticalTextAlignEnum vAlign = VerticalTextAlignEnum.TOP;
		HorizontalTextAlignEnum hAlign = HorizontalTextAlignEnum.LEFT;

		JRTextAlignment alignment = element instanceof JRTextAlignment ? (JRTextAlignment)element : null;
		if (alignment != null)
		{
			vAlign = alignment.getVerticalTextAlign();
			hAlign = alignment.getHorizontalTextAlign();
		}
		
		horizontalAlignment = ParagraphStyle.getHorizontalAlignment(hAlign, vAlign, rotation);
		verticalAlignment = ParagraphStyle.getVerticalAlignment(hAlign, vAlign, rotation);
		this.shrinkToFit = shrinkToFit;
		this.wrapText = wrapText;
		setBox(gridCell.getBox());
	}
	
	@Override
	public String getId()
	{
		return backcolor + super.getId() + "|" + horizontalAlignment + "|" + verticalAlignment + "|" + shrinkToFit + "|" + wrapText; 
	}

	@Override
	public void write(String cellStyleName)
	{
		styleWriter.write("<style:style style:name=\"");
		styleWriter.write(cellStyleName);
		styleWriter.write("\"");
		styleWriter.write(" style:family=\"table-cell\">\n");
		styleWriter.write(" <style:table-cell-properties");	
		styleWriter.write(" style:shrink-to-fit=\""	+ shrinkToFit + "\"");
		styleWriter.write(" fo:wrap-option=\"" + (!wrapText ||  shrinkToFit ? "no-" : "") + "wrap\"");
		if (backcolor != null)
		{
			styleWriter.write(" fo:background-color=\"#");
			styleWriter.write(backcolor);
			styleWriter.write("\"");
		}
		
		writeBorder(TOP_BORDER);
		writeBorder(LEFT_BORDER);
		writeBorder(BOTTOM_BORDER);
		writeBorder(RIGHT_BORDER);
		
		if (verticalAlignment != null)
		{
			styleWriter.write(" style:vertical-align=\"");
			styleWriter.write(verticalAlignment);
			styleWriter.write("\"");
		}

		styleWriter.write("/>\n");

		if (horizontalAlignment != null)
		{
			styleWriter.write(" <style:paragraph-properties");		
			styleWriter.write(" fo:text-align=\"");
			styleWriter.write(horizontalAlignment);
			styleWriter.write("\"");
			styleWriter.write("/>\n");
		}

		styleWriter.write("</style:style>\n");
	}

}
