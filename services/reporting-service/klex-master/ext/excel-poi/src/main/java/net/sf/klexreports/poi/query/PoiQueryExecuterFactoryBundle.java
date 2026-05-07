/*
 * Copyright (C) 2005 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 * Licensed under commercial Klexsoft Subscription License Agreement
 */
package net.sf.klexreports.poi.query;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.query.JRQueryExecuterFactoryBundle;
import net.sf.klexreports.engine.query.QueryExecuterFactory;
import net.sf.klexreports.engine.util.JRSingletonCache;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PoiQueryExecuterFactoryBundle implements JRQueryExecuterFactoryBundle {
	private static final JRSingletonCache<QueryExecuterFactory> cache = new JRSingletonCache<QueryExecuterFactory>(
			QueryExecuterFactory.class);
	private static final PoiQueryExecuterFactoryBundle INSTANCE = new PoiQueryExecuterFactoryBundle();
	private static final String[] LANGUAGES = new String[] { "xls", "XLS", "xlsx", "XLSX" };

	private PoiQueryExecuterFactoryBundle() {
	}

	/**
	 * 
	 */
	public static PoiQueryExecuterFactoryBundle getInstance() {
		return INSTANCE;
	}

	@Override
	public String[] getLanguages() {
		return LANGUAGES;
	}

	@Override
	public QueryExecuterFactory getQueryExecuterFactory(String language) throws JRException {
		for (String lang : getLanguages()) {
			if (lang.equalsIgnoreCase(language))
				return cache.getCachedInstance(ExcelQueryExecuterFactory.class.getName());
		}
		return null;
	}
}
