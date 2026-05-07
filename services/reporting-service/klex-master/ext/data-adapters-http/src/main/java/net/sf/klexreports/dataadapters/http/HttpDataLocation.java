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
package net.sf.klexreports.dataadapters.http;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import net.sf.klexreports.data.DataFile;
import net.sf.klexreports.data.http.HttpLocationParameter;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface HttpDataLocation extends DataFile
{

	RequestMethod getMethod();
	
	String getUrl();
	
	String getUsername();
	
	String getPassword();
	
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonProperty("urlParameter")
	List<HttpLocationParameter> getUrlParameters();
	
	String getBody();

	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonProperty("postParameter")
	List<HttpLocationParameter> getPostParameters();
	
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonProperty("header")
	List<HttpLocationParameter> getHeaders();

}
