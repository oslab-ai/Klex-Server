/*
 * Copyright (C) 2005 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 * Licensed under commercial Klexsoft Subscription License Agreement
 */
package net.sf.klexreports.olap;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.query.JRQueryExecuterFactoryBundle;
import net.sf.klexreports.engine.query.QueryExecuterFactory;
import net.sf.klexreports.engine.util.JRSingletonCache;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class OlapQueryExecuterFactoryBundle implements JRQueryExecuterFactoryBundle {
	private static final JRSingletonCache<QueryExecuterFactory> cache = new JRSingletonCache<QueryExecuterFactory>(
			QueryExecuterFactory.class);
	private static final OlapQueryExecuterFactoryBundle INSTANCE = new OlapQueryExecuterFactoryBundle();
	private static final String[] LANGUAGES = new String[] { "mdx", "MDX", "olap4j", "OLAP4J" };

	private OlapQueryExecuterFactoryBundle() {
	}

	/**
	 * 
	 */
	public static OlapQueryExecuterFactoryBundle getInstance() {
		return INSTANCE;
	}

	@Override
	public String[] getLanguages() {
		return LANGUAGES;
	}

	@Override
	public QueryExecuterFactory getQueryExecuterFactory(String language) throws JRException 
	{
		language = language.toUpperCase();
		if (language.equals("MDX"))
		{
			return cache.getCachedInstance(JRMdxQueryExecuterFactory.class.getName());
		}
		if (language.equals("OLAP4J"))
		{
			return cache.getCachedInstance(Olap4jQueryExecuterFactory.class.getName());
		}
		return null;
	}
}
