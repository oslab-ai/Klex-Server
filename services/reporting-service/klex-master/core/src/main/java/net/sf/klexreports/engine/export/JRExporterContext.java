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
package net.sf.klexreports.engine.export;

import java.util.Map;

import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.export.Exporter;
import net.sf.klexreports.repo.RepositoryUtil;


/**
 * A context that represents information about an export process.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface JRExporterContext
{
	/**
	 * Returns the current exporter.
	 * 
	 * @return current exporter
	 */
	Exporter getExporterRef();

	/**
	 *
	 */
	public KlexReportsContext getKlexReportsContext();
	
	default public RepositoryUtil getRepository()
	{
		return RepositoryUtil.getInstance(getKlexReportsContext());
	}

	/**
	 * Returns the report which is currently exported.
	 * 
	 * @return currently exported report
	 */
	KlexPrint getExportedReport();

	/**
	 * Returns the current X-axis offset at which elements should be exported.
	 * 
	 * @return the current X-axis offset
	 */
	int getOffsetX();

	/**
	 * Returns the current Y-axis offset at which elements should be exported.
	 * 
	 * @return the current Y-axis offset
	 */
	int getOffsetY();

	/**
	 *
	 */
	public Object getValue(String key);

	/**
	 *
	 */
	public void setValue(String key, Object value);

	/**
	 *
	 */
	public Map<String, Object> getValues();
}
