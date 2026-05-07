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
package net.sf.klexreports.engine.export.ooxml;

import java.awt.Color;
import java.io.Writer;

import net.sf.klexreports.engine.JRBoxContainer;
import net.sf.klexreports.engine.JRImageAlignment;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRTextAlignment;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.JRExporterGridCell;
import net.sf.klexreports.engine.type.ModeEnum;
import net.sf.klexreports.engine.type.RotationEnum;
import net.sf.klexreports.engine.type.VerticalImageAlignEnum;
import net.sf.klexreports.engine.type.VerticalTextAlignEnum;
import net.sf.klexreports.engine.util.JRColorUtil;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class DocxCellHelper extends BaseHelper
{
	/**
	 *
	 */
	private static final String VERTICAL_ALIGN_TOP = "top";
	private static final String VERTICAL_ALIGN_MIDDLE = "center";
	private static final String VERTICAL_ALIGN_BOTTOM = "bottom";
	
	/**
	 *
	 */
	private DocxBorderHelper borderHelper;
	
	/**
	 *
	 */
	public DocxCellHelper(KlexReportsContext klexReportsContext, Writer writer)
	{
		super(klexReportsContext, writer);
		
		borderHelper = new DocxBorderHelper(klexReportsContext, writer);
	}

	/**
	 *
	 */
	public void exportHeader(JRPrintElement element, JRExporterGridCell gridCell) 
	{
		write("    <w:tc>\n");
		
		exportPropsHeader();

		if (gridCell.getColSpan() > 1)
		{
			write("      <w:gridSpan w:val=\"" + gridCell.getColSpan() +"\" />\n");
		}
		if (gridCell.getRowSpan() > 1)
		{
			write("      <w:vMerge w:val=\"restart\" />\n");
		}
		
		exportProps(element, gridCell);
		
		exportPropsFooter();
	}

	/**
	 *
	 */
	public void exportFooter() 
	{
		write("    </w:tc>\n");
	}


	/**
	 *
	 */
	public void exportProps(JRPrintElement element, JRExporterGridCell gridCell)
	{
		exportBackcolor(ModeEnum.OPAQUE, gridCell.getCellBackcolor());
		
		borderHelper.exportBorder(gridCell.getBox());
		if (element instanceof JRBoxContainer)
		{
			borderHelper.exportPadding(((JRBoxContainer)element).getLineBox());
		}

//		if (element instanceof JRCommonGraphicElement)
//			borderHelper.export(((JRCommonGraphicElement)element).getLinePen());
		
		JRTextAlignment align = element instanceof JRTextAlignment ? (JRTextAlignment)element : null;
		if (align != null)
		{
			JRPrintText text = element instanceof JRPrintText ? (JRPrintText)element : null;
			RotationEnum rotation = text == null ? null : text.getRotation();
			
			String verticalAlignment = 
				getVerticalAlignment(
					align.getVerticalTextAlign() 
					);
			String textRotation = getTextDirection(rotation);

			exportAlignmentAndRotation(verticalAlignment, textRotation);
		}
		else if (element instanceof JRImageAlignment)
		{
			JRImageAlignment iAlign = (JRImageAlignment)element;
			if (iAlign != null)
			{
				JRPrintImage image = element instanceof JRPrintImage ? (JRPrintImage)element : null;
				String verticalAlignment = getVerticalImageAlign(image.getVerticalImageAlign());
				exportAlignmentAndRotation(verticalAlignment, null);
			}
		}
	}


	/**
	 *
	 */
	public void exportProps(JRExporterGridCell gridCell)
	{
		exportBackcolor(ModeEnum.OPAQUE, gridCell.getCellBackcolor());
		
		borderHelper.exportBorder(gridCell.getBox());
	}

	
	/**
	 *
	 */
	private void exportBackcolor(ModeEnum mode, Color backcolor)
	{
		if (mode == ModeEnum.OPAQUE && backcolor != null)
		{
			write("      <w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"" + JRColorUtil.getColorHexa(backcolor) + "\" />\n");
		}
	}

	/**
	 *
	 */
	private void exportPropsHeader()
	{
		write("      <w:tcPr>\n");
	}
	
	/**
	 *
	 */
	private void exportAlignmentAndRotation(String verticalAlignment, String textRotation)
	{
		if (verticalAlignment != null)
		{
			write("      <w:vAlign w:val=\"" + verticalAlignment +"\" />\n");
		}
		if (textRotation != null)
		{
			write("   <w:textDirection w:val=\"" + textRotation + "\" />\n");
		}
	}
	
	/**
	 *
	 */
	private void exportPropsFooter()
	{
		write("      </w:tcPr>\n");
	}
	
	/**
	 *
	 */
	private static String getTextDirection(RotationEnum rotation)
	{
		String textDirection = null;
		
		if (rotation != null)
		{
			switch(rotation)
			{
				case LEFT:
				{
					textDirection = "btLr";
					break;
				}
				case RIGHT:
				{
					textDirection = "tbRl";
					break;
				}
				case UPSIDE_DOWN://FIXMEDOCX possible?
				case NONE:
				default:
				{
				}
			}
		}

		return textDirection;
	}

	/**
	 *
	 */
	public static String getVerticalAlignment(VerticalTextAlignEnum verticalAlignment)
	{
		if (verticalAlignment != null)
		{
			switch (verticalAlignment)
			{
				case BOTTOM :
					return VERTICAL_ALIGN_BOTTOM;
				case MIDDLE :
					return VERTICAL_ALIGN_MIDDLE;
				case TOP :
				case JUSTIFIED : //DOCX does not support vertical text justification
				default :
					return VERTICAL_ALIGN_TOP;
			}
		}
		return null;
	}
	
	/**
	 *
	 */
	public static String getVerticalImageAlign(VerticalImageAlignEnum verticalAlignment)
	{
		if (verticalAlignment != null)
		{
			switch (verticalAlignment)
			{
				case BOTTOM :
					return VERTICAL_ALIGN_BOTTOM;
				case MIDDLE :
					return VERTICAL_ALIGN_MIDDLE;
				case TOP :
				default :
					return VERTICAL_ALIGN_TOP;
			}
		}
		return null;
	}
}
