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

import net.sf.klexreports.crosstabs.JRCrosstabRowGroup;
import net.sf.klexreports.crosstabs.design.JRDesignCrosstab;
import net.sf.klexreports.crosstabs.design.JRDesignCrosstabBucket;
import net.sf.klexreports.crosstabs.design.JRDesignCrosstabRowGroup;
import net.sf.klexreports.crosstabs.interactive.CrosstabOrderAttributes;
import net.sf.klexreports.crosstabs.interactive.SortRowGroupData;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.analytics.dataset.BucketOrder;
import net.sf.klexreports.engine.fill.JRFillCrosstab;
import net.sf.klexreports.interactivity.commands.Command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SortRowGroupCommand implements Command
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private static final Log log = LogFactory.getLog(SortRowGroupCommand.class);

	private final JRDesignCrosstab crosstab;
	private final SortRowGroupData sortData;
	
	private JRDesignCrosstabRowGroup rowGroup;
	private boolean lastRowGroup;
	
	private CrosstabOrderAttributes oldOrderAttributes;
	private BucketOrder newOrder;

	public SortRowGroupCommand(JRDesignCrosstab crosstab, SortRowGroupData sortData)
	{
		this.crosstab = crosstab;
		this.sortData = sortData;
	}
	
	@Override
	public void execute()
	{
		JRCrosstabRowGroup[] rowGroups = crosstab.getRowGroups();
		rowGroup = (JRDesignCrosstabRowGroup) rowGroups[sortData.getGroupIndex()];
		lastRowGroup = sortData.getGroupIndex() == rowGroups.length - 1;

		oldOrderAttributes = new CrosstabOrderAttributes(crosstab);
		
		newOrder = sortData.getOrder();
		
		setOrder();
	}

	protected void setOrder()
	{
		oldOrderAttributes.prepareSorting();

		if (log.isDebugEnabled())
		{
			log.debug("setting crosstab " + sortData.getCrosstabId() + " row group " + sortData.getGroupIndex() 
					+ " order to " + newOrder);
		}
		
		((JRDesignCrosstabBucket) rowGroup.getBucket()).setOrder(newOrder);
		
		if (lastRowGroup && newOrder != BucketOrder.NONE)
		{
			// sorting the last row group, we need to reset the column order by
			crosstab.getPropertiesMap().setProperty(JRFillCrosstab.PROPERTY_ORDER_BY_COLUMN, null);
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
