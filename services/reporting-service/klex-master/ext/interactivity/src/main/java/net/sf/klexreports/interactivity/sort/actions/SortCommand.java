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
package net.sf.klexreports.interactivity.sort.actions;

import java.util.List;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRExpressionChunk;
import net.sf.klexreports.engine.JRGroup;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRSortField;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.design.JRDesignDataset;
import net.sf.klexreports.engine.design.JRDesignSortField;
import net.sf.klexreports.engine.type.SortFieldTypeEnum;
import net.sf.klexreports.engine.type.SortOrderEnum;
import net.sf.klexreports.interactivity.commands.Command;
import net.sf.klexreports.interactivity.commands.CommandException;
import net.sf.klexreports.interactivity.commands.CommandStack;
import net.sf.klexreports.interactivity.headertoolbar.HeaderToolbarElementUtils;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class SortCommand implements Command 
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	/**
	 * Property that specifies whether additional sort fields should be created automatically to preserve the integrity of dataset groups,
	 * when interactive sorting is performed. The groups need to have simple expressions using single field or single variable reference, for
	 * this feature to work properly.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_TABLE,
			defaultValue = PropertyConstants.BOOLEAN_FALSE,
			scopes = {PropertyScope.CONTEXT, PropertyScope.DATASET},
			sinceVersion = PropertyConstants.VERSION_4_6_0,
			valueType = Boolean.class
			)
	public static final String PROPERTY_CREATE_SORT_FIELDS_FOR_GROUPS = JRPropertiesUtil.PROPERTY_PREFIX + "create.sort.fields.for.groups";
	
//	private ReportContext reportContext;
	private KlexReportsContext klexReportsContext;
	protected JRDesignDataset dataset;
	protected SortData sortData;
//	protected KlexDesignCache cache;
	private CommandStack individualCommandStack;
	
	public SortCommand(KlexReportsContext klexReportsContext, JRDesignDataset dataset, SortData sortData) 
	//public AbstractSortCommand(ReportContext reportContext, SortData sortData) 
	{
//		this.reportContext = reportContext;
		this.klexReportsContext = klexReportsContext;
		this.dataset = dataset;
		this.sortData = sortData;
//		this.cache = KlexDesignCache.getInstance(reportContext);
		this.individualCommandStack = new CommandStack();
	}

	@Override
	public void execute() throws CommandException 
	{
		SortOrderEnum sortOrder = HeaderToolbarElementUtils.getSortOrder(sortData.getSortOrder());//FIXMEJIVE use labels in JR enum, even if they are longer

		JRDesignSortField newSortField = 
			new JRDesignSortField(
				sortData.getSortColumnName(),
				SortFieldTypeEnum.getByName(sortData.getSortColumnType()),
				sortOrder
				);
		
		JRSortField oldSortField = null;
		List<JRSortField> sortFields = dataset.getSortFieldsList();
		if (
			JRPropertiesUtil.getInstance(klexReportsContext).getBooleanProperty(dataset, PROPERTY_CREATE_SORT_FIELDS_FOR_GROUPS, false)
			&& (sortFields == null || sortFields.isEmpty())
			)
		{
			List<JRGroup> groups = dataset.getGroupsList();
			for (JRGroup group : groups)
			{
				JRExpression expression = group.getExpression();
				if (expression != null)
				{
					JRExpressionChunk[] chunks = expression.getChunks();
					if (chunks != null && chunks.length == 1)
					{
						JRExpressionChunk chunk = chunks[0];
						if (
							chunk.getType() == JRExpressionChunk.TYPE_FIELD
							|| chunk.getType() == JRExpressionChunk.TYPE_VARIABLE
							)
						{
							JRDesignSortField groupSortField = 
								new JRDesignSortField(
									chunk.getText(),
									chunk.getType() == JRExpressionChunk.TYPE_FIELD 
										? SortFieldTypeEnum.FIELD
										: SortFieldTypeEnum.VARIABLE,
									SortOrderEnum.ASCENDING
									);
							individualCommandStack.execute(new AddSortFieldCommand(dataset, groupSortField));
						}
					}
				}
			}
		}

		for (JRSortField crtSortField : sortFields)
		{
			if (
				newSortField.getName().equals(crtSortField.getName())
				&& newSortField.getType() == crtSortField.getType()
				)
			{
				oldSortField = crtSortField;
				break;
			}
		}

		if (oldSortField != null)
		{
			individualCommandStack.execute(new RemoveSortFieldCommand(dataset, oldSortField));
		}
		
		if (sortOrder != null)
		{
			individualCommandStack.execute(new AddSortFieldCommand(dataset, newSortField));
		}
	}
	
	@Override
	public void undo() 
	{
		individualCommandStack.undoAll();
	}

	@Override
	public void redo() 
	{
		individualCommandStack.redoAll();
	}
}
