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

import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.export.CutsInfo;
import net.sf.klexreports.engine.export.JRExporterGridCell;
import net.sf.klexreports.engine.export.LengthUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PptxTableHelper extends BaseHelper
{
	/**
	 * 
	 */
	private CutsInfo xCuts;
	private PptxCellHelper cellHelper;
	private PptxParagraphHelper paragraphHelper;

	/**
	 * 
	 */
	protected PptxTableHelper(
		KlexReportsContext klexReportsContext,
		Writer writer,
		CutsInfo xCuts
		) 
	{
		super(klexReportsContext, writer);

		this.xCuts = xCuts;
		this.cellHelper = new PptxCellHelper(klexReportsContext, writer);
		this.paragraphHelper = new PptxParagraphHelper(klexReportsContext, writer);
	}


	/**
	 * 
	 */
	public PptxCellHelper getCellHelper() 
	{
		return cellHelper;
	}
	

	/**
	 * 
	 */
	public PptxParagraphHelper getParagraphHelper() 
	{
		return paragraphHelper;
	}
	

	/**
	 * 
	 */
	public void exportHeader() 
	{
		write("  <a:tbl>\n");
		write("   <a:tblPr>\n");
		write("   <a:tableStyleId>{2D5ABB26-0587-4C30-8999-92F81FD0307C}</a:tableStyleId>\n");
		write("   </a:tblPr>\n");
		write("   <a:tblGrid>\n");
		for(int col = 1; col < xCuts.size(); col++)
		{
			write("    <a:gridCol w=\"" + LengthUtil.emu(xCuts.getCutOffset(col) - xCuts.getCutOffset(col - 1)) + "\"/>\n");
		}
		write("   </a:tblGrid>\n");
	}
	
	public void exportFooter() 
	{
		write("  </a:tbl>\n");
	}
	
	public void exportRowHeader(int rowHeight) 
	{
		write("   <a:tr h=\"" + LengthUtil.emu(rowHeight) + "\">\n");
	}
	
	public void exportRowFooter() 
	{
		write("   </a:tr>\n");
	}
	
	public void exportEmptyCell(
		JRExporterGridCell gridCell, 
		JRExporterGridCell topGridCell, 
		JRExporterGridCell leftGridCell, 
		JRExporterGridCell rightGridCell, 
		JRExporterGridCell bottomGridCell 
		)
	{
		cellHelper.exportHeader(gridCell);
		paragraphHelper.exportEmptyParagraph();
		write("  <a:tcPr marL=\"0\" marR=\"0\" marT=\"0\" marB=\"0\">\n");
		cellHelper.getBorderHelper().exportBorder(gridCell, topGridCell, leftGridCell, rightGridCell, bottomGridCell);
		cellHelper.exportBackcolor(gridCell);
		write("  </a:tcPr>\n");
		cellHelper.exportFooter();
	}
	
	public void exportOccupiedCells(
		JRExporterGridCell gridCell,
		JRExporterGridCell topGridCell,
		JRExporterGridCell leftGridCell,
		JRExporterGridCell rightGridCell,
		JRExporterGridCell bottomGridCell
		)
	{
		write("    <a:tc hMerge=\"1\">\n");
//		write("    <a:tc" + (gridCell.getColSpan() > 1 ? (" gridSpan=\"" + gridCell.getColSpan() +"\"") : "") + " vMerge=\"1\">\n");
		
		paragraphHelper.exportEmptyParagraph();
	
		write("  <a:tcPr marL=\"0\" marR=\"0\" marT=\"0\" marB=\"0\">\n");
		cellHelper.getBorderHelper().exportBorder(gridCell, topGridCell, leftGridCell, rightGridCell, bottomGridCell);
		cellHelper.exportBackcolor(gridCell);
		write("  </a:tcPr>\n");

		cellHelper.exportFooter();
	}
}