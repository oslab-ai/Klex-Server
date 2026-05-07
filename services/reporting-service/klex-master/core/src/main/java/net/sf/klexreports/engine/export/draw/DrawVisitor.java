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
 * Eugene D - eugenedruy@users.sourceforge.net 
 * Adrian Jackson - iapetus@users.sourceforge.net
 * David Taylor - exodussystems@users.sourceforge.net
 * Lars Kristensen - llk@users.sourceforge.net
 */
package net.sf.klexreports.engine.export.draw;

import java.awt.Graphics2D;

import net.sf.klexreports.engine.JRBreak;
import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.JRElementGroup;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPrintText;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.convert.ConvertVisitor;
import net.sf.klexreports.engine.convert.ReportConverter;
import net.sf.klexreports.engine.util.JRStyledText;
import net.sf.klexreports.engine.util.UniformElementVisitor;
import net.sf.klexreports.export.Graphics2DReportConfiguration;
import net.sf.klexreports.renderers.RenderersCache;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class DrawVisitor extends UniformElementVisitor
{

	protected ConvertVisitor convertVisitor;
	protected PrintDrawVisitor drawVisitor;
	
	/**
	 *
	 */
	public DrawVisitor(ReportConverter reportConverter, Graphics2D grx)
	{
		this.convertVisitor = new ConvertVisitor(reportConverter);
		
		KlexReportsContext klexReportsContext = reportConverter.getKlexReportsContext();
		JRReport report = reportConverter.getReport();
		JRPropertiesUtil propUtil = JRPropertiesUtil.getInstance(klexReportsContext);
		
		this.drawVisitor = 
			new PrintDrawVisitor(
				klexReportsContext,
				new RenderersCache(klexReportsContext),
				propUtil.getBooleanProperty(report, Graphics2DReportConfiguration.MINIMIZE_PRINTER_JOB_SIZE, true),
				propUtil.getBooleanProperty(report, JRStyledText.PROPERTY_AWT_IGNORE_MISSING_FONT, false),
				propUtil.getBooleanProperty(report, JRPrintText.PROPERTY_AWT_INDENT_FIRST_LINE, true),
				propUtil.getBooleanProperty(report, JRPrintText.PROPERTY_AWT_JUSTIFY_LAST_LINE, false)
				);
		
		setGraphics2D(grx);
		this.drawVisitor.setClip(true);
	}
	public void setClip(boolean clip)
	{
		this.drawVisitor.setClip(clip);
	}

	/**
	 *
	 */
	public void setGraphics2D(Graphics2D grx)
	{
		drawVisitor.setGraphics2D(grx);
	}

	@Override
	public void visitBreak(JRBreak breakElement)
	{
		//FIXMEDRAW
	}

	@Override
	protected void visitElement(JRElement element)
	{
		JRPrintElement printElement = convertVisitor.getVisitPrintElement(element);
		printElement.accept(drawVisitor, elementOffset(element));
	}

	protected Offset elementOffset(JRElement element)
	{
		return new Offset(-element.getX(), -element.getY());
	}
	
	@Override
	public void visitElementGroup(JRElementGroup elementGroup)
	{
		//nothing to draw. elements are drawn individually.
	}

}
