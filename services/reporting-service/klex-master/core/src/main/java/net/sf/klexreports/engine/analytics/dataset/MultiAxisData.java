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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.klexreports.engine.JRCloneable;
import net.sf.klexreports.engine.analytics.data.Axis;
import net.sf.klexreports.engine.xml.JRXmlConstants;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
@JsonDeserialize(as = DesignMultiAxisData.class)
public interface MultiAxisData extends JRCloneable
{
	
	MultiAxisDataset getDataset();
	
	@JsonGetter("dataAxes")
	@JacksonXmlProperty(localName = JRXmlConstants.ELEMENT_dataAxis)
	@JacksonXmlElementWrapper(useWrapping = false)
	List<DataAxis> getDataAxisList();
	
	DataAxis getDataAxis(Axis axis);
	
	@JacksonXmlProperty(localName = "measure")
	@JacksonXmlElementWrapper(useWrapping = false)
	List<DataMeasure> getMeasures();

}
