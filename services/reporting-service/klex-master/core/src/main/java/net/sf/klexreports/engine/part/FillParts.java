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
package net.sf.klexreports.engine.part;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.klexreports.engine.JRPart;
import net.sf.klexreports.engine.JRSection;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class FillParts
{

	private List<FillPart> parts;
	
	public FillParts(JRSection section, JRFillObjectFactory fillFactory)
	{
		JRPart[] sectionParts = section == null ? null : section.getParts();
		if (sectionParts == null || sectionParts.length == 0)
		{
			parts = Collections.emptyList();
		}
		else
		{
			parts = new ArrayList<>(sectionParts.length);
			for (JRPart part : sectionParts)
			{
				FillPart fillPart = new FillPart(part, fillFactory);
				parts.add(fillPart);
			}
		}
	}

	public List<FillPart> getParts()
	{
		return parts;
	}

}
