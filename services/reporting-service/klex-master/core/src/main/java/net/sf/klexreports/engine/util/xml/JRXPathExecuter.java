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
package net.sf.klexreports.engine.util.xml;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.data.JRXmlDataSource;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * An XPath expression executer.
 * <p/>
 * {@link JRXmlDataSource XML data sources} delegate XPath executions to implementations
 * of this interface.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see JRXPathExecuterFactory
 * @see JRXPathExecuterUtils
 */
public interface JRXPathExecuter
{

	/**
	 * Selects a node list by evaluating an XPath expression on a context node.
	 * 
	 * @param contextNode the context node (a document can also be used)
	 * @param expression the XPath expression
	 * @return the selected node list
	 * @throws JRException if the XPath evaluation failed
	 */
	NodeList selectNodeList(Node contextNode, String expression) throws JRException;

	
	/**
	 * Selects an object by evaluating an XPath expression on a context node.
	 * <p/>
	 * If the expression evaluates to a node list, the first node in the list should be returned.
	 * Otherwise, the primitive value resulted from the evaluation should be returned as a
	 * <code>java.lang.String</code>, <code>java.lang.Number</code> or <code>java.lang.Boolean</code>.
	 * 
	 * @param contextNode the context node (a document can also be used)
	 * @param expression the XPath expression
	 * @return the selected node or value
	 * @throws JRException if the XPath evaluation failed
	 */
	Object selectObject(Node contextNode, String expression) throws JRException;

}
