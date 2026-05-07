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

import java.util.Map;
import java.util.Set;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.interactivity.commands.CommandException;
import net.sf.klexreports.interactivity.commands.CommandTarget;
import net.sf.klexreports.interactivity.commands.ResetInCacheCommand;
import net.sf.klexreports.repo.KlexDesignCache;
import net.sf.klexreports.repo.KlexDesignReportResource;
import net.sf.klexreports.web.util.WebConstants;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class SaveZoomAction extends AbstractAction {
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private String zoomValue;

	public String getZoomValue() {
		return zoomValue;
	}

	public void setZoomValue(String zoomValue) {
		this.zoomValue = zoomValue;
	}

	@Override
	public void performAction() throws ActionException {
		if (zoomValue != null && zoomValue.length() > 0) {
			CommandTarget target = getCommandTarget(getKlexReportsContext(), getReportContext());
			if (target != null) {
				// execute command
				try {
					getCommandStack().execute(
						new ResetInCacheCommand(
								new SaveZoomCommand((KlexDesign)target.getIdentifiable(), zoomValue),
								getKlexReportsContext(),
								getReportContext(),
								target.getUri()
						)
					);
				} catch (CommandException e) {
					throw new ActionException(e);
				}
			}
		} else {
			errors.addAndThrow("net.sf.klexreports.web.actions.empty.zoom");
		}
	}

	@Override
	public boolean requiresRefill() {
		return false;
	}

	public CommandTarget getCommandTarget(KlexReportsContext klexReportsContext, ReportContext reportContext) {
		KlexDesignCache cache = KlexDesignCache.getInstance(klexReportsContext, reportContext);
		Map<String, KlexDesignReportResource> cachedResources = cache.getCachedResources();

		Set<String> uris = cachedResources.keySet();
		String reportUri = (String) reportContext.getParameterValue(WebConstants.REQUEST_PARAMETER_REPORT_URI);

		if (reportUri != null) {
			for (String uri : uris) {
				if (reportUri.equals(uri)) {
					CommandTarget target = new CommandTarget();
					target.setUri(uri);
					target.setIdentifiable(cache.getKlexDesign(uri));
					return target;
				}
			}
		}

		return null;
	}

}