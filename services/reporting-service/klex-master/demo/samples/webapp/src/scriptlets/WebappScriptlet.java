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
package scriptlets;

import net.sf.klexreports.engine.JRDefaultScriptlet;
import net.sf.klexreports.engine.JRScriptletException;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class WebappScriptlet extends JRDefaultScriptlet
{


	@Override
	public void afterGroupInit(String groupName) throws JRScriptletException
	{
		String allCities = (String)this.getVariableValue("AllCities");
		String city = (String)this.getFieldValue("City");
		StringBuilder sb = new StringBuilder();
		
		if (allCities != null)
		{
			sb.append(allCities);
			sb.append(", ");
		}
		
		sb.append(city);
		this.setVariableValue("AllCities", sb.toString());
	}


	/**
	 *
	 */
	public String hello() throws JRScriptletException
	{
		return "Hello! I'm the report's scriptlet object.";
	}


}
