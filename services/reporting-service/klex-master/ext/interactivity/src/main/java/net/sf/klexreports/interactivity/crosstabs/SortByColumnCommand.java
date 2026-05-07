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
package net.sf.klexreports.interactivity.crosstabs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.crosstabs.JRCrosstabRowGroup;
import net.sf.klexreports.crosstabs.design.JRDesignCrosstab;
import net.sf.klexreports.crosstabs.design.JRDesignCrosstabBucket;
import net.sf.klexreports.crosstabs.interactive.CrosstabOrderAttributes;
import net.sf.klexreports.crosstabs.interactive.SortByColumnData;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.analytics.dataset.BucketOrder;
import net.sf.klexreports.engine.fill.JRFillCrosstab;
import net.sf.klexreports.interactivity.commands.Command;
import net.sf.klexreports.interactivity.commands.CommandException;
import net.sf.klexreports.jackson.util.JacksonUtil;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SortByColumnCommand implements Command
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private static final Log log = LogFactory.getLog(SortByColumnCommand.class);

	private final KlexReportsContext klexReportsContext;
	private final JRDesignCrosstab crosstab;
	private final SortByColumnData sortData;
	private final JRCrosstabRowGroup lastRowGroup;
	
	private CrosstabOrderAttributes oldOrderAttributes;
	private String newOrderBy;

	public SortByColumnCommand(KlexReportsContext klexReportsContext, JRDesignCrosstab crosstab, SortByColumnData sortData)
	{
		this.klexReportsContext = klexReportsContext;
		this.crosstab = crosstab;
		this.sortData = sortData;
		
		JRCrosstabRowGroup[] rowGroups = crosstab.getRowGroups();
		lastRowGroup = rowGroups[rowGroups.length - 1];
	}
	
	@Override
	public void execute() throws CommandException
	{
		oldOrderAttributes = new CrosstabOrderAttributes(crosstab);
		
		BucketOrder order = sortData.getOrder();
		if (order == BucketOrder.NONE)
		{
			newOrderBy = null;
		}
		else
		{
			OrderByColumnInfoImpl orderByInfo = new OrderByColumnInfoImpl();
			orderByInfo.setMeasureIndex(sortData.getMeasureIndex()); 
			orderByInfo.setOrder(BucketOrder.toSortOrderEnum(order));
			orderByInfo.setColumnValues(sortData.getColumnValues());
			newOrderBy = JacksonUtil.getInstance(klexReportsContext).getJsonString(orderByInfo);
		}

		setOrder();
	}

	protected void setOrder()
	{
		oldOrderAttributes.prepareSorting();
		
		if (log.isDebugEnabled())
		{
			log.debug("setting crosstab " + sortData.getCrosstabId() + " order by to " + newOrderBy);
		}
		
		if (newOrderBy == null)
		{
			crosstab.getPropertiesMap().removeProperty(JRFillCrosstab.PROPERTY_ORDER_BY_COLUMN);
		}
		else
		{
			crosstab.getPropertiesMap().setProperty(JRFillCrosstab.PROPERTY_ORDER_BY_COLUMN, newOrderBy);
			
			// clearing the order of the last row group so that the ordering does not reappear when unsorting by the column
			((JRDesignCrosstabBucket) lastRowGroup.getBucket()).setOrder(BucketOrder.NONE);
		}
	}

	@Override
	public void undo()
	{
		oldOrderAttributes.restore();
	}

	@Override
	public void redo()
	{
		setOrder();
	}

}
