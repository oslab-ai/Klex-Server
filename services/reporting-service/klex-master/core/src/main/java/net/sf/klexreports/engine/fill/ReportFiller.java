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
package net.sf.klexreports.engine.fill;

import java.sql.Connection;
import java.util.Map;

import org.apache.commons.javaflow.api.continuable;

import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.KlexPrint;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface ReportFiller
{

	@continuable
	KlexPrint fill(Map<String, Object> parameters, Connection connection) throws JRException;

	@continuable
	KlexPrint fill(Map<String, Object> parameters, JRDataSource dataSource) throws JRException;

	@continuable
	KlexPrint fill(Map<String, Object> parameters) throws JRException;

	void addFillListener(FillListener listener);

	void cancelFill() throws JRException;

	boolean isPageFinal(int pageIndex);

	JRFillContext getFillContext();

}
