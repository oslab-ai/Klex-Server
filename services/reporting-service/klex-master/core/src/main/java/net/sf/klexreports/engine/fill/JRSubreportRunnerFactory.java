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

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * Factory of {@link net.sf.klexreports.engine.fill.JRSubreportRunner JRSubreportRunner} instances.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface JRSubreportRunnerFactory
{
	/**
	 * Property specifying the {@link net.sf.klexreports.engine.fill.JRSubreportRunnerFactory JRSubreportRunnerFactory}
	 * implementation to use for creating subreport runners.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			defaultValue = "net.sf.klexreports.engine.fill.JRThreadSubreportRunnerFactory",
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_1_2_2
			)
	public static final String SUBREPORT_RUNNER_FACTORY = JRPropertiesUtil.PROPERTY_PREFIX + "subreport.runner.factory";
	
	/**
	 * Creates a new {@link net.sf.klexreports.engine.fill.JRSubreportRunner JRSubreportRunner} instance.
	 * 
	 * @param fillSubreport the subreport element of the master report
	 * @param subreportFiller the subreport filler created to fill the subreport
	 * @return a new {@link net.sf.klexreports.engine.fill.JRSubreportRunner JRSubreportRunner} instance
	 */
	JRSubreportRunner createSubreportRunner(JRFillSubreport fillSubreport, JRBaseFiller subreportFiller);
}
