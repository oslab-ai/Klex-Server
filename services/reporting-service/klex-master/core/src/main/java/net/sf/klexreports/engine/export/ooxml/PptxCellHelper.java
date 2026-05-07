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

import java.io.Writer;

import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.JRExporterGridCell;
import net.sf.klexreports.engine.type.RotationEnum;
import net.sf.klexreports.engine.type.VerticalImageAlignEnum;
import net.sf.klexreports.engine.type.VerticalTextAlignEnum;
import net.sf.klexreports.engine.util.JRColorUtil;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class PptxCellHelper extends BaseHelper
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
	private PptxBorderHelper borderHelper;
	
	/**
	 *
	 */
	public PptxCellHelper(KlexReportsContext klexReportsContext, Writer writer)
	{
		super(klexReportsContext, writer);
		
		borderHelper = new PptxBorderHelper(klexReportsContext, writer);
	}
	
	/**
	 * 
	 */
	public PptxBorderHelper getBorderHelper()
	{
		return borderHelper;
	}
	
	/**
	 *
	 */
	public void exportHeader(JRExporterGridCell gridCell)
	{
		write("    <a:tc");
		if (gridCell.getColSpan() > 1)
		{
			write(" gridSpan=\"" + gridCell.getColSpan() +"\"");
		}
		if (gridCell.getRowSpan() > 1)
		{
			write(" rowSpan=\"" + gridCell.getRowSpan() + "\"");
		}
		write(">\n");
	}

	/**
	 *
	 */
	public void exportFooter() 
	{
		write("    </a:tc>\n");
	}

	/**
	 *
	 */
	public void exportBackcolor(JRExporterGridCell gridCell) 
	{
		if (gridCell.getCellBackcolor() != null)
		{
			write("  <a:solidFill>\n");
			write("    <a:srgbClr val=\"" + JRColorUtil.getColorHexa(gridCell.getCellBackcolor()) + "\"/>\n");
			write("  </a:solidFill>\n");
		}
	}

	/**
	 *
	 */
	public void exportAlignmentAndRotation(JRPrintText text)
	{
		RotationEnum rotation = text == null ? null : text.getRotation();
		
		String verticalAlignment = 
			getVerticalAlignment(
				text.getVerticalTextAlign() 
				);
		String textRotation = getTextDirection(rotation);

		exportAlignmentAndRotation(verticalAlignment, textRotation);
	}

	/**
	 *
	 */
	public void exportAlignmentAndRotation(JRPrintImage image)
	{
		String verticalAlignment = getVerticalImageAlign(image.getVerticalImageAlign());
		exportAlignmentAndRotation(verticalAlignment, null);
	}

	/**
	 *
	 */
	private void exportAlignmentAndRotation(String verticalAlignment, String textRotation)
	{
		if (verticalAlignment != null)
		{
			write("      <a:vAlign a:val=\"" + verticalAlignment +"\" />\n");
		}
		if (textRotation != null)
		{
			write("   <a:textDirection a:val=\"" + textRotation + "\" />\n");
		}
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
