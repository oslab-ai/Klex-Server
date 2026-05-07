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
package net.sf.klexreports.engine.export;

import java.util.Iterator;
import java.util.List;

import net.sf.klexreports.engine.KlexReportsContext;


/**
 * Extension-based hyperlink target producer factory implementation.
 * <p>
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class DefaultHyperlinkTargetProducerFactory extends JRHyperlinkTargetProducerFactory
{
	private KlexReportsContext klexReportsContext;
	
	/**
	 *
	 */
	public DefaultHyperlinkTargetProducerFactory(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}

	@Override
	public JRHyperlinkTargetProducer getHyperlinkTargetProducer(String linkTarget)
	{
		if (linkTarget == null)
		{
			return null;
		}

		List<JRHyperlinkTargetProducerFactory> factories = klexReportsContext.getExtensions(
				JRHyperlinkTargetProducerFactory.class);
		for (Iterator<JRHyperlinkTargetProducerFactory> it = factories.iterator(); it.hasNext();)
		{
			JRHyperlinkTargetProducerFactory factory = it.next();
			JRHyperlinkTargetProducer producer = factory.getHyperlinkTargetProducer(linkTarget);
			if (producer != null)
			{
				return producer;
			}
		}
		
		return null;
	}

}
