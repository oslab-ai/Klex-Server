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

import java.util.UUID;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.design.JRDesignDataset;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.DefaultFormatFactory;
import net.sf.klexreports.engine.util.FormatFactory;
import net.sf.klexreports.interactivity.actions.AbstractAction;
import net.sf.klexreports.interactivity.actions.ActionException;
import net.sf.klexreports.interactivity.commands.CommandException;
import net.sf.klexreports.interactivity.commands.CommandStack;
import net.sf.klexreports.interactivity.commands.CommandTarget;
import net.sf.klexreports.interactivity.commands.ResetInCacheCommand;
import net.sf.klexreports.repo.KlexDesignCache;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class FilterAction extends AbstractAction {
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private FilterData filterData;
	protected static FormatFactory formatFactory = new DefaultFormatFactory();
	
	public FilterAction() {
	}

	public FilterData getFilterData() {
		return filterData;
	}

	public void setFilterData(FilterData filterData) {
		this.filterData = filterData;
	}

	@Override
	public void performAction() throws ActionException {
		
		if (filterData != null) {
			CommandTarget target = getCommandTarget(UUID.fromString(filterData.getTableUuid()));
			if (target != null)
			{
				KlexDesignCache cache = KlexDesignCache.getInstance(getKlexReportsContext(), getReportContext());

				KlexDesign klexDesign = cache.getKlexDesign(target.getUri());
				JRDesignDataset dataset = (JRDesignDataset)klexDesign.getMainDataset();
				
				// obtain command stack
				CommandStack commandStack = getCommandStack();
				
				// execute command
				try {
					commandStack.execute(
						new ResetInCacheCommand(
							new FilterCommand(getKlexReportsContext(), dataset, getFilterData()),
							getKlexReportsContext(),
							getReportContext(), 
							target.getUri()
							)
						);
				} catch (CommandException e) {
					 throw new ActionException(e);
				}
			}
		}
	}
	
}
