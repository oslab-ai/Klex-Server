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
package net.sf.klexreports.repo;

import java.util.List;

import net.sf.klexreports.engine.KlexReportsContext;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class PersistenceUtil
{
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private PersistenceUtil(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	public static PersistenceUtil getInstance(KlexReportsContext klexReportsContext)
	{
		return new PersistenceUtil(klexReportsContext);
	}
	
	
	/**
	 * 
	 */
	public PersistenceService getService(Class<? extends RepositoryService> repositoryServiceType, Class<? extends Resource> resourceType)
	{
		List<PersistenceServiceFactory> factories = klexReportsContext.getExtensions(PersistenceServiceFactory.class);
		for (PersistenceServiceFactory factory : factories)
		{
			PersistenceService service = factory.getPersistenceService(klexReportsContext, repositoryServiceType, resourceType);
			if (service != null)
			{
				return service;
			}
		}
		//throw new JRRuntimeException("No persistence service registered for the '" + repositoryServiceType.getName() + "' repository type and '" + resourceType.getName() + "' resource type.");
		return null;
	}
}
