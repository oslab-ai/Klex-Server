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
package com.update;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.SimpleKlexReportsContext;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.ReportUpdater;
import net.sf.klexreports.engine.xml.JRXmlLoader;
import net.sf.klexreports.engine.xml.JRXmlWriter;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class RenewUuidsUpdater implements ReportUpdater 
{
	
	@Override
	public KlexDesign update(KlexDesign klexDesign)
	{
		SimpleKlexReportsContext localContext = new SimpleKlexReportsContext();
		localContext.setProperty(JRXmlWriter.PROPERTY_EXCLUDE_UUIDS, Boolean.TRUE.toString());
		JRXmlWriter writer = new JRXmlWriter(localContext);
		String jrxml = writer.write(klexDesign, StandardCharsets.UTF_8.name());
		
		try
		{
			klexDesign = JRXmlLoader.load(new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8)));
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
		
		return klexDesign;
	}
}
