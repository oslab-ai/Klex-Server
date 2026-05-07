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
package net.sf.klexreports.interactivity.actions;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.JRSaver;
import net.sf.klexreports.repo.KlexDesignCache;
import net.sf.klexreports.repo.KlexDesignReportResource;



/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class SaveAction extends AbstractAction {
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	public SaveAction() {
	}

	public String getName() {
		return "save_action";
	}

	@Override
	public void performAction() 
	{
//		KlexDesign klexDesign = getKlexDesign();
		KlexDesignCache cache = KlexDesignCache.getInstance(getKlexReportsContext(), getReportContext());
		Map<String, KlexDesignReportResource> cachedResources = cache.getCachedResources();
		for (Entry<String, KlexDesignReportResource> entry : cachedResources.entrySet())
		{
			String uri = entry.getKey();
			KlexDesignReportResource resource = entry.getValue();
			KlexDesign klexDesign = resource.getKlexDesign();
			if (klexDesign != null)
			{
				KlexReport klexReport = resource.getReport();
				String appRealPath = null;//FIXMECONTEXT WebFileRepositoryService.getApplicationRealPath();
				try
				{
					JRSaver.saveObject(klexReport, new File(new File(new File(appRealPath), "WEB-INF/repository"), uri));//FIXMEJIVE harcoded
				}
				catch (JRException e)
				{
					throw new JRRuntimeException(e);
				}
			}
		}
	}

}
