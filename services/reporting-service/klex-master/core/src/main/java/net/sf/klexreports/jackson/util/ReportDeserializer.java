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
package net.sf.klexreports.jackson.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import net.sf.klexreports.engine.JRDefaultStyleProvider;
import net.sf.klexreports.engine.JRReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.design.KlexDesign;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ReportDeserializer extends StdDeserializer<JRReport>
{
	private static final long serialVersionUID = 1L;
	
	private KlexReportsContext klexReportsContext;
	private static final ThreadLocal<JRDefaultStyleProvider> defaultStyleProvider = new ThreadLocal<JRDefaultStyleProvider>();

	public ReportDeserializer(KlexReportsContext klexReportsContext)
	{
		this((Class<?>)null);
		
		this.klexReportsContext = klexReportsContext;
	}
	
	public ReportDeserializer(Class<?> vc)
	{
		super(vc);
	}

	@Override
	public JRReport deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException 
	{
		KlexDesign klexDesign = new KlexDesign(klexReportsContext);
		defaultStyleProvider.set(klexDesign);
		return klexDesign;
    }
	
	public static JRDefaultStyleProvider getDefaultStyleProvider()
	{
		return defaultStyleProvider.get();
	}
}
