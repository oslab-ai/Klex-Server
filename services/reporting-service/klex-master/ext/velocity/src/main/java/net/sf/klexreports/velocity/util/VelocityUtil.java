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
package net.sf.klexreports.velocity.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.properties.PropertyConstants;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;


/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class VelocityUtil
{
	@Property(
			name = "net.sf.klexreports.velocity.{arbitrary_suffix}",
			category = PropertyConstants.CATEGORY_OTHER,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_4_7_1
			)
	public static final String VELOCITY_PROPERTY_PREFIX = JRPropertiesUtil.PROPERTY_PREFIX + "velocity.";
	private static final VelocityEngine velocityEngine;
	
	static {
		velocityEngine = new VelocityEngine();
		
		JRPropertiesUtil propertiesUtil = JRPropertiesUtil.getInstance(DefaultKlexReportsContext.getInstance());
		List<PropertySuffix> properties = propertiesUtil.getProperties(VELOCITY_PROPERTY_PREFIX);
		
		for (PropertySuffix property: properties) {
			velocityEngine.setProperty(property.getSuffix(), property.getValue());
		}
		
		velocityEngine.init();
	}
	
	public static VelocityEngine getVelocityEngine() {
		return velocityEngine;
	}
	
	public static String processTemplate(String templateName, VelocityContext vContext) {
		Template template = getVelocityEngine().getTemplate(templateName, "UTF-8");

		StringWriter writer = new StringWriter(128);
		template.merge(vContext, writer);
		writer.flush();
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return writer.getBuffer().toString();
	}
	
	public static String processTemplate(String templateName, Map<String, Object> contextMap) {
		return processTemplate(templateName, new VelocityContext(contextMap));
	}
}
