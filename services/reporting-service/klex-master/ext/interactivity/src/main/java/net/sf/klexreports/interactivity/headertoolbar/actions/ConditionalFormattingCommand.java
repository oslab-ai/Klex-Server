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
package net.sf.klexreports.interactivity.headertoolbar.actions;

import net.sf.klexreports.components.headertoolbar.HeaderToolbarElement;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRTextField;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.interactivity.commands.Command;
import net.sf.klexreports.jackson.util.JacksonUtil;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class ConditionalFormattingCommand implements Command 
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private KlexReportsContext klexReportsContext;
	protected ConditionalFormattingData conditionalFormattingData;
	private String oldSerializedConditionsData;
	private String newSerializedConditionsData;
	private JRTextField textElement;

	public ConditionalFormattingCommand(KlexReportsContext klexReportsContext, JRTextField textElement, ConditionalFormattingData conditionalFormattingData)
	{
		this.klexReportsContext = klexReportsContext;
		this.textElement = textElement;
		this.conditionalFormattingData = conditionalFormattingData;
	}

	@Override
	public void execute() 
	{
		if (textElement != null)
		{
			// get existing condition data as JSON string
			String serializedConditionData = null;
			JRPropertiesMap propertiesMap = textElement.getPropertiesMap();
			if (propertiesMap.containsProperty(HeaderToolbarElement.COLUMN_CONDITIONAL_FORMATTING_PROPERTY)) {
				serializedConditionData = propertiesMap.getProperty(HeaderToolbarElement.COLUMN_CONDITIONAL_FORMATTING_PROPERTY);
			}
			
			oldSerializedConditionsData = serializedConditionData;
			
			JacksonUtil jacksonUtil = JacksonUtil.getInstance(klexReportsContext);
//			ConditionalFormattingData existingConditionData = jacksonUtil.loadObject(serializedConditionData, ConditionalFormattingData.class);
//			if (existingConditionData != null) {
//				existingConditionData.setConditions(conditionalFormattingData.getConditions());
//			} else {
//				existingConditionData = conditionalFormattingData;
//			}
//			
//			newSerializedConditionsData = jacksonUtil.getJsonString(existingConditionData);
			newSerializedConditionsData = jacksonUtil.getJsonString(conditionalFormattingData);
			propertiesMap.setProperty(HeaderToolbarElement.COLUMN_CONDITIONAL_FORMATTING_PROPERTY, newSerializedConditionsData);
		}
	}
	
	@Override
	public void undo() 
	{
		if (textElement != null) 
		{
			textElement.getPropertiesMap().setProperty(HeaderToolbarElement.COLUMN_CONDITIONAL_FORMATTING_PROPERTY, oldSerializedConditionsData);
		}
	}

	@Override
	public void redo() 
	{
		if (textElement != null) 
		{
			textElement.getPropertiesMap().setProperty(HeaderToolbarElement.COLUMN_CONDITIONAL_FORMATTING_PROPERTY, newSerializedConditionsData);
		}			
	}
}
