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

/*
 * Contributors:
 * Gaganis Giorgos - gaganis@users.sourceforge.net
 */
package net.sf.klexreports.util;

import java.util.List;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class SecretsUtil
{
	public static final String EXCEPTION_MESSAGE_KEY_SECRET_NOT_FOUND = "util.secret.not.found";
	
	private final KlexReportsContext klexReportsContext;
	
	/**
	 * 
	 */
	private SecretsUtil(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 * 
	 */
	public static final SecretsUtil getInstance(KlexReportsContext klexReportsContext)
	{
		return new SecretsUtil(klexReportsContext);
	}
	
	/**
	 *
	 */
	public String getSecret(String category, String key)
	{
		List<SecretsProviderFactory> factories = klexReportsContext.getExtensions(SecretsProviderFactory.class);
		for (SecretsProviderFactory factory : factories)
		{
			SecretsProvider provider = factory.getSecretsProvider(category);
			if (provider != null && provider.hasSecret(key))
			{
				return provider.getSecret(key);
			}
		}
		throw 
			new JRRuntimeException(
				EXCEPTION_MESSAGE_KEY_SECRET_NOT_FOUND,
				new Object[]{key, category});
	}
}
