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
package net.sf.klexreports.engine.type;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * Defines specific moments in time when the default value expression of a parameter is supposed to be evaluated.
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public enum ParameterEvaluationTimeEnum implements NamedEnum
{
	/**
	 * Evaluate the parameter default value expression before parameter contributors.
	 */
	EARLY("Early"),
	/**
	 * Evaluate the parameter default value expression after parameter contributors.
	 */
	LATE("Late");
	
	/**
	 * Provides a default value for the {@link JRParameter#getEvaluationTime()} property of parameters and can be set either globally or at dataset level.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			defaultValue = "Late",
			scopes = {PropertyScope.CONTEXT, PropertyScope.DATASET},
			sinceVersion = PropertyConstants.VERSION_6_3_1,
			valueType = ParameterEvaluationTimeEnum.class
			)
	public static final String PROPERTY_EVALUATION_TIME = JRPropertiesUtil.PROPERTY_PREFIX + "parameter.evaluation.time";

	private final String name;
	
	private ParameterEvaluationTimeEnum(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public static ParameterEvaluationTimeEnum byName(String name)
	{
		return EnumUtil.getEnumByName(values(), name);
	}
}
