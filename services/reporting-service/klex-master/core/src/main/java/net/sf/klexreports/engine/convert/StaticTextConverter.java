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
package net.sf.klexreports.engine.convert;

import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRStaticText;
import net.sf.klexreports.engine.base.JRBasePrintText;
import net.sf.klexreports.engine.util.JRTextMeasurerUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class StaticTextConverter extends TextElementConverter
{

	/**
	 *
	 */
	private final static StaticTextConverter INSTANCE = new StaticTextConverter();
	
	/**
	 *
	 */
	private StaticTextConverter()
	{
	}

	/**
	 *
	 */
	public static StaticTextConverter getInstance()
	{
		return INSTANCE;
	}
	
	@Override
	public JRPrintElement convert(ReportConverter reportConverter, JRElement element)
	{
		JRBasePrintText printText = new JRBasePrintText(reportConverter.getDefaultStyleProvider());
		JRStaticText staticText = (JRStaticText)element;
		
		copyTextElement(reportConverter, staticText, printText);
		
		printText.setText(staticText.getText());
		
		JRTextMeasurerUtil.getInstance(reportConverter.getKlexReportsContext()).measureTextElement(printText);
		
		return printText;
	}

}
