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
package net.sf.klexreports.engine.util;

/**
 * An XML namespace.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see JRXmlWriteHelper#startElement(String, XmlNamespace)
 */
public class XmlNamespace
{

	private final String nsURI;
	private final String prefix;
	private final String schemaURI;
	
	/**
	 * Creates an XML namespace.
	 * 
	 * @param uri the namespace URI
	 * @param prefix the namespace prefix
	 * @param schemaURI the URI of the XML schema associated with the namespace
	 */
	public XmlNamespace(String uri, String prefix, String schemaURI)
	{
		this.prefix = prefix;
		this.schemaURI = schemaURI;
		this.nsURI = uri;
	}
	
	/**
	 * Returns the namespace URI.
	 * 
	 * @return the namespace URI
	 */
	public String getNamespaceURI()
	{
		return nsURI;
	}

	/**
	 * Returns the namespace prefix.
	 * 
	 * @return the namespace prefix
	 */
	public String getPrefix()
	{
		return prefix;
	}

	/**
	 * Returns the URI of the XML schema associated with the namespace.
	 * 
	 * @return the namespace XML schema URI
	 */
	public String getSchemaURI()
	{
		return schemaURI;
	}
	
}
