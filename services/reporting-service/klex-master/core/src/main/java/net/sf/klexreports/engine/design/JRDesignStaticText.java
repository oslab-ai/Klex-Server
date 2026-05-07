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
package net.sf.klexreports.engine.design;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.JRStaticText;
import net.sf.klexreports.engine.JRVisitor;
import net.sf.klexreports.engine.base.JRBaseStaticText;


/**
 * This class is used for representing a design static text element. It contains the full functionality needed
 * to use static texts at design time.
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRDesignStaticText extends JRDesignTextElement implements JRStaticText
{


	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */
	protected String text;


	/**
	 *
	 */
	public JRDesignStaticText()
	{
		super(null);
	}
		
	/**
	 *
	 */
	public JRDesignStaticText(JRDefaultStyleProvider defaultStyleProvider)
	{
		super(defaultStyleProvider);
	}
		

	@Override
	public String getText()
	{
		return this.text;
	}

	@Override
	public void setText(String text)
	{
		Object old = this.text;
		this.text = text;
		getEventSupport().firePropertyChange(JRBaseStaticText.PROPERTY_TEXT, old, this.text);
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		collector.collect(this);
	}

	@Override
	public void visit(JRVisitor visitor)
	{
		visitor.visitStaticText(this);
	}

}
