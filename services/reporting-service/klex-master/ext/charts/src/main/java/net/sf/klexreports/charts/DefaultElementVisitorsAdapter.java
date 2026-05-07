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
package net.sf.klexreports.charts;

import net.sf.klexreports.charts.base.ChartsBaseObjectFactory;
import net.sf.klexreports.charts.convert.ChartsConvertVisitor;
import net.sf.klexreports.charts.design.ChartsVerifier;
import net.sf.klexreports.charts.fill.ChartsFillObjectFactory;
import net.sf.klexreports.charts.util.ChartsApiWriter;
import net.sf.klexreports.engine.JRVisitor;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.convert.ConvertVisitor;
import net.sf.klexreports.engine.design.JRVerifierVisitor;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;
import net.sf.klexreports.engine.util.JRApiWriterVisitor;
import net.sf.klexreports.engine.util.JRElementsVisitor;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DefaultElementVisitorsAdapter implements ElementVisitorAdapter
{

	private static final DefaultElementVisitorsAdapter INSTANCE = new DefaultElementVisitorsAdapter();
	
	public static DefaultElementVisitorsAdapter instance()
	{
		return INSTANCE;
	}
	
	@Override
	public ChartVisitor getChartVisitor(JRVisitor visitor)
	{
		if (visitor instanceof JRElementsVisitor)
		{
			return new ChartsElementsVisitor((JRElementsVisitor)visitor);
		}
		else if (visitor instanceof JRBaseObjectFactory)
		{
			return new ChartsBaseObjectFactory((JRBaseObjectFactory)visitor);
		}
		else if (visitor instanceof JRFillObjectFactory)
		{
			return new ChartsFillObjectFactory((JRFillObjectFactory)visitor);
		}
		else if (visitor instanceof JRVerifierVisitor)
		{
			return new ChartsVerifier((JRVerifierVisitor)visitor);
		}
		else if (visitor instanceof JRApiWriterVisitor)
		{
			return new ChartsApiWriter((JRApiWriterVisitor)visitor);
		}
		else if (visitor instanceof ConvertVisitor)
		{
			return new ChartsConvertVisitor(((ConvertVisitor)visitor));
		}
		return  null;
	}

}
