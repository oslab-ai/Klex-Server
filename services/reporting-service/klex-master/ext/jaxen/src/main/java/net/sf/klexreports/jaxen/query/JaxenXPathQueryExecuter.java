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
package net.sf.klexreports.jaxen.query;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import net.sf.klexreports.engine.JRDataSource;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.klexreports.engine.query.JRAbstractQueryExecuter;
import net.sf.klexreports.engine.query.JRXPathQueryExecuter;
import net.sf.klexreports.engine.query.QueryExecutionContext;
import net.sf.klexreports.engine.query.SimpleQueryExecutionContext;
import net.sf.klexreports.jaxen.data.JaxenXmlDataSource;
import net.sf.klexreports.engine.JRValueParameter;
import net.sf.klexreports.engine.KlexReportsContext;

/**
 * Jaxen XPath query executer implementation.
 * <p/>
 * The XPath query of the report is executed against the document specified by the
 * {@link net.sf.klexreports.jaxen.query.JaxenXPathQueryExecuterFactory#PARAMETER_XML_DATA_DOCUMENT PARAMETER_XML_DATA_DOCUMENT}
 * parameter.
 * <p/>
 * All the parameters in the XPath query are replaced by calling <code>String.valueOf(Object)</code>
 * on the parameter value.
 * 
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JaxenXPathQueryExecuter extends JRAbstractQueryExecuter
{
	private static final Log log = LogFactory.getLog(JaxenXPathQueryExecuter.class);
	
	private final Document document;
	
	private final DocumentBuilderFactory documentBuilderFactory;
	
	private Map<String, String> namespacesMap;

	/**
	 *
	 */
	public JaxenXPathQueryExecuter(
		KlexReportsContext klexReportsContext,
		JRDataset dataset,
		Map<String,? extends JRValueParameter> parametersMap
		)
	{
		this(SimpleQueryExecutionContext.of(klexReportsContext), dataset, parametersMap);
	}
	
	/**
	 * 
	 */
	public JaxenXPathQueryExecuter(
		QueryExecutionContext context,
		JRDataset dataset, 
		Map<String,? extends JRValueParameter> parametersMap
		)
	{
		super(context, dataset, parametersMap);
		
		document = (Document) getParameterValue(JaxenXPathQueryExecuterFactory.PARAMETER_XML_DATA_DOCUMENT);
		documentBuilderFactory = (DocumentBuilderFactory) getParameterValue(
				JaxenXPathQueryExecuterFactory.PARAMETER_DOCUMENT_BUILDER_FACTORY, true);
		namespacesMap = (Map<String, String>) getParameterValue(
				JaxenXPathQueryExecuterFactory.PARAMETER_XML_NAMESPACE_MAP, true);
		
		if (document == null)
		{
			log.warn("The supplied org.w3c.dom.Document object is null.");
		}

		parseQuery();
	}

	@Override
	protected String getCanonicalQueryLanguage()
	{
		return JRXPathQueryExecuter.CANONICAL_LANGUAGE;
	}

	@Override
	protected String getParameterReplacement(String parameterName)
	{
		return String.valueOf(getParameterValue(parameterName));
	}

	@Override
	public JRDataSource createDatasource() throws JRException
	{
		JaxenXmlDataSource datasource = null;
		
		String xPath = getQueryString();
		
		if (log.isDebugEnabled())
		{
			log.debug("XPath query: " + xPath);
		}
		
		if (document != null && xPath != null)
		{
			if (namespacesMap == null)
			{
				namespacesMap = extractXmlNamespacesFromProperties();
			}
			
			datasource = new JaxenXmlDataSource(document, xPath);
			
			datasource.setXmlNamespaceMap(namespacesMap);
			datasource.setDetectXmlNamespaces(getBooleanParameterOrProperty(JaxenXPathQueryExecuterFactory.XML_DETECT_NAMESPACES, false));
			datasource.setDocumentBuilderFactory(documentBuilderFactory);
			
			datasource.setLocale((Locale)getParameterValue(JaxenXPathQueryExecuterFactory.XML_LOCALE, true));
			datasource.setDatePattern(getStringParameter(JaxenXPathQueryExecuterFactory.XML_DATE_PATTERN, JaxenXPathQueryExecuterFactory.PROPERTY_XML_DATE_PATTERN));
			datasource.setNumberPattern(getStringParameter(JaxenXPathQueryExecuterFactory.XML_NUMBER_PATTERN, JaxenXPathQueryExecuterFactory.PROPERTY_XML_NUMBER_PATTERN));
			datasource.setTimeZone((TimeZone)getParameterValue(JaxenXPathQueryExecuterFactory.XML_TIME_ZONE, true));
		}
		
		return datasource;
	}

	@Override
	public void close()
	{
		//nothing to do
	}

	@Override
	public boolean cancelQuery() throws JRException
	{
		//nothing to cancel
		return false;
	}
	
	private Map<String, String> extractXmlNamespacesFromProperties() throws JRException 
	{
		Map<String, String> namespaces = new HashMap<>();
		
		String xmlnsPrefix = JaxenXPathQueryExecuterFactory.XML_NAMESPACE_PREFIX;
		List<PropertySuffix> nsProperties = JRPropertiesUtil.getProperties(dataset, xmlnsPrefix);
		
		for (int i=0; i < nsProperties.size(); ++i) 
		{
			PropertySuffix prop = nsProperties.get(i);
			String nsPrefix = prop.getKey().substring(xmlnsPrefix.length());
			if (nsPrefix.length() > 0) 
			{
				namespaces.put(nsPrefix, prop.getValue());
			}
		}
		
		return namespaces.size()>0 ? namespaces : null;
	}
}