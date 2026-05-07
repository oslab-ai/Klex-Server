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

import java.util.UUID;

import net.sf.klexreports.crosstabs.design.JRDesignCrosstab;
import net.sf.klexreports.crosstabs.interactive.SortRowGroupData;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.interactivity.actions.AbstractAction;
import net.sf.klexreports.interactivity.actions.ActionException;
import net.sf.klexreports.interactivity.commands.CommandException;
import net.sf.klexreports.interactivity.commands.CommandTarget;
import net.sf.klexreports.interactivity.commands.ResetInCacheCommand;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SortRowGroupAction extends AbstractAction
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private static final Log log = LogFactory.getLog(SortRowGroupAction.class);

	private SortRowGroupData sortData;
	
	@Override
	public void performAction() throws ActionException
	{
		CommandTarget target = getCommandTarget(UUID.fromString(sortData.getCrosstabId()), JRDesignCrosstab.class);
		if (target != null)
		{
			JRDesignCrosstab crosstab = (JRDesignCrosstab) target.getIdentifiable();
			try
			{
				SortRowGroupCommand sortCommand = new SortRowGroupCommand(crosstab, getSortData());
				ResetInCacheCommand command = new ResetInCacheCommand(sortCommand,
					getKlexReportsContext(), getReportContext(), target.getUri());
				getCommandStack().execute(command);
			}
			catch (CommandException e)
			{
				if (log.isDebugEnabled())
				{
					log.debug("error sorting crosstab", e);
				}
				
				throw new ActionException(e);
			}
		}
	}

	public SortRowGroupData getSortData()
	{
		return sortData;
	}

	public void setSortData(SortRowGroupData sortData)
	{
		this.sortData = sortData;
	}

}
