/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.components.table;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.components.ComponentsExtensionsRegistryFactory;
import net.sf.klexreports.engine.DatasetRunHolder;
import net.sf.klexreports.engine.JRCloneable;
import net.sf.klexreports.engine.JRDatasetRun;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRVisitable;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.xml.JRXmlConstants;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
@JsonTypeName(ComponentsExtensionsRegistryFactory.TABLE_COMPONENT_NAME)
@JsonDeserialize(as = StandardTable.class)
public interface TableComponent extends Component, JRCloneable, JRVisitable, DatasetRunHolder
{
	/**
	 * Property that specifies a default value for the <code>whenNoDataType</code> attribute of table components.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_TABLE,
			defaultValue = "Blank",
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT},
			sinceVersion = PropertyConstants.VERSION_6_0_0,
			valueType = WhenNoDataTypeTableEnum.class
			)
	public static final String CONFIG_PROPERTY_WHEN_NO_DATA_TYPE = JRPropertiesUtil.PROPERTY_PREFIX + "components.table.when.no.data.type";

	@Override
	JRDatasetRun getDatasetRun();

	@JacksonXmlProperty(localName = "column")
	@JacksonXmlElementWrapper(useWrapping = false)
	List<BaseColumn> getColumns();
	
	@JacksonXmlProperty(isAttribute = true)
	WhenNoDataTypeTableEnum getWhenNoDataType();
	
	Row getTableHeader();
	
	Row getTableFooter();
	
	@JacksonXmlProperty(localName = JRXmlConstants.ELEMENT_groupHeader)
	@JacksonXmlElementWrapper(useWrapping = false)
	List<GroupRow> getGroupHeaders();
	
	Row getGroupHeader(String groupName);
	
	@JacksonXmlProperty(localName = JRXmlConstants.ELEMENT_groupFooter)
	@JacksonXmlElementWrapper(useWrapping = false)
	List<GroupRow> getGroupFooters();
	
	Row getGroupFooter(String groupName);
	
	Row getColumnHeader();
	
	Row getColumnFooter();
	
	Row getDetail();
	
	BaseCell getNoData();
}
