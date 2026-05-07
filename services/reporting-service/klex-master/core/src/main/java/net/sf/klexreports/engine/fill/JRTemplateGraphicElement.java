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

import net.sf.klexreports.engine.JRCommonGraphicElement;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRGraphicElement;
import net.sf.klexreports.engine.JROrigin;
import net.sf.klexreports.engine.JRPen;
import net.sf.klexreports.engine.type.FillEnum;
import net.sf.klexreports.engine.util.ObjectUtils;
import net.sf.klexreports.engine.util.ObjectUtils.HashCode;


/**
 * Base class consisting of graphic print element information shared by multiple
 * print element instances.
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class JRTemplateGraphicElement extends JRTemplateElement implements JRCommonGraphicElement
{
	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */
	protected JRPen linePen;
	private FillEnum fill;


	/**
	 *
	 */
	protected JRTemplateGraphicElement(JROrigin origin, JRDefaultStyleProvider defaultStyleProvider)
	{
		super(origin, defaultStyleProvider);
	}

	/**
	 *
	 */
	protected JRTemplateGraphicElement(JROrigin origin, JRDefaultStyleProvider defaultStyleProvider, JRGraphicElement graphicElement)
	{
		super(origin, defaultStyleProvider);

		setGraphicElement(graphicElement);
	}


	/**
	 *
	 */
	protected void setGraphicElement(JRGraphicElement graphicElement)
	{
		super.setElement(graphicElement);
		
		copyLinePen(graphicElement.getLinePen());
		
		setFill(graphicElement.getOwnFill());
	}

	/**
	 * Copies {@link JRPen pen} attributes.
	 * 
	 * @param pen the object to copy the attributes from
	 */
	public void copyLinePen(JRPen pen)
	{
		linePen = pen.clone(this);
	}

	@Override
	public JRPen getLinePen()
	{
		return linePen;
	}
		

	@Override
	public FillEnum getFill()
	{
		return getStyleResolver().getFill(this);
	}

	@Override
	public FillEnum getOwnFill()
	{
		return this.fill;
	}
	
	@Override
	public void setFill(FillEnum fill)
	{
		this.fill = fill;
	}

	@Override
	public Float getDefaultLineWidth() 
	{
		return JRPen.LINE_WIDTH_1;
	}

	@Override
	public Color getDefaultLineColor() 
	{
		return getForecolor();
	}

	protected void addGraphicHash(HashCode hash)
	{
		addTemplateHash(hash);
		hash.addIdentical(linePen);
		hash.add(fill);
	}

	protected boolean graphicIdentical(JRTemplateGraphicElement template)
	{
		return templateIdentical(template)
				&& ObjectUtils.identical(linePen, template.linePen)
				&& ObjectUtils.equals(fill, template.fill);
	}
	
	@Override
	public void populateStyle()
	{
		super.populateStyle();
		
		if (linePen != null)
		{
			linePen.populateStyle();
		}
		
		fill = getFill();
	}
}
