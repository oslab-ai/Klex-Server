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
package net.sf.klexreports.engine.analytics.dataset;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.klexreports.engine.JRCloneable;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.type.CalculationEnum;
import net.sf.klexreports.engine.xml.JRXmlConstants;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
@JsonDeserialize(as = DesignDataMeasure.class)
public interface DataMeasure extends JRCloneable
{

	@JacksonXmlProperty(isAttribute = true)
	String getName();
	
	JRExpression getLabelExpression();
	
	@JsonGetter(JRXmlConstants.ATTRIBUTE_class)
	@JacksonXmlProperty(localName = JRXmlConstants.ATTRIBUTE_class, isAttribute = true)
	String getValueClassName();
	
	@JsonIgnore
	Class<?> getValueClass();
	
	JRExpression getValueExpression();
	
	@JacksonXmlProperty(isAttribute = true)
	CalculationEnum getCalculation();
	
	@JsonGetter(JRXmlConstants.ATTRIBUTE_incrementerFactoryClass)
	@JacksonXmlProperty(localName = JRXmlConstants.ATTRIBUTE_incrementerFactoryClass, isAttribute = true)
	String getIncrementerFactoryClassName();
	
	@JsonIgnore
	Class<?> getIncrementerFactoryClass();
	
}
