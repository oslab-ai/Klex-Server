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

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.design.JRDesignDataset;
import net.sf.klexreports.engine.design.JRDesignDatasetRun;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.interactivity.actions.ActionException;
import net.sf.klexreports.interactivity.commands.CommandException;
import net.sf.klexreports.interactivity.commands.ResetInCacheCommand;
import net.sf.klexreports.interactivity.sort.actions.SortCommand;
import net.sf.klexreports.interactivity.sort.actions.SortData;
import net.sf.klexreports.repo.KlexDesignCache;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class SortAction extends AbstractVerifiableTableAction {
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public SortAction() {
	}

	public SortData getSortData() {
		return (SortData)columnData;
	}

	public void setSortData(SortData sortData) {
		columnData = sortData;
	}

	@Override
	public void performAction() throws ActionException {
		JRDesignDatasetRun datasetRun = (JRDesignDatasetRun)table.getDatasetRun();
		
		String datasetName = datasetRun.getDatasetName();
		
		KlexDesignCache cache = KlexDesignCache.getInstance(getKlexReportsContext(), getReportContext());

		KlexDesign klexDesign = cache.getKlexDesign(targetUri);
		JRDesignDataset dataset = (JRDesignDataset)klexDesign.getDatasetMap().get(datasetName);
		
		// execute command
		try {
			getCommandStack().execute(
				new ResetInCacheCommand(
					new SortCommand(getKlexReportsContext(), dataset, getSortData()),
					getKlexReportsContext(),
					getReportContext(), 
					targetUri
					)
				);
		} catch (CommandException e) {
			throw new ActionException(e);
		}
	}

	@Override
	public void verify() throws ActionException {
	}
}
