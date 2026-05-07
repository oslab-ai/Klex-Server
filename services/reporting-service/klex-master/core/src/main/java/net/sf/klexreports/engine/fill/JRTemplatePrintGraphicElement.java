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
package net.sf.klexreports.engine.fill;

import java.awt.Color;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRPen;
import net.sf.klexreports.engine.JRPrintGraphicElement;
import net.sf.klexreports.engine.type.FillEnum;


/**
 * Base implementation of {@link net.sf.klexreports.engine.JRPrintGraphicElement} that uses
 * a {@link net.sf.klexreports.engine.fill.JRTemplateGraphicElement} instance to
 * store common attributes. 
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRTemplatePrintGraphicElement extends JRTemplatePrintElement implements JRPrintGraphicElement
{


	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public JRTemplatePrintGraphicElement()
	{
		
	}
	
	/**
	 *
	 * @param originator
	 */
	protected JRTemplatePrintGraphicElement(JRTemplateGraphicElement graphicElement, PrintElementOriginator originator)
	{
		super(graphicElement, originator);
	}

	@Override
	public JRPen getLinePen()
	{
		return ((JRTemplateGraphicElement)template).getLinePen();
	}
		

	@Override
	public FillEnum getFill()
	{
		return ((JRTemplateGraphicElement)this.template).getFill();
	}

	@Override
	public FillEnum getOwnFill()
	{
		return ((JRTemplateGraphicElement)this.template).getOwnFill();
	}

	@Override
	public void setFill(FillEnum fill)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Float getDefaultLineWidth() 
	{
		return ((JRTemplateGraphicElement)template).getDefaultLineWidth();
	}

	@Override
	public Color getDefaultLineColor() 
	{
		return ((JRTemplateGraphicElement)template).getDefaultLineColor();
	}

	
}
