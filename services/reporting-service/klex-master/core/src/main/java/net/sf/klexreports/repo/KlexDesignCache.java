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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.JRStyle;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.Pair;
import net.sf.klexreports.engine.xml.JRXmlLoader;
import net.sf.klexreports.engine.xml.JRXmlWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class KlexDesignCache implements Serializable
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private static final Log log = LogFactory.getLog(KlexDesignCache.class);
	
	public static final String EXCEPTION_MESSAGE_KEY_INVALID_ENTRY = "repo.invalid.entry";
	
	/**
	 * 
	 */
	private static final String PARAMETER_KLEX_DESIGN_CACHE = "net.sf.klexreports.parameter.klexdesign.cache";

	/**
	 * 
	 */
	private KlexReportsContext klexReportsContext;
	private ReportCompiler reportCompiler;
	private Map<String, KlexDesignReportResource> cachedResourcesMap = new ConcurrentHashMap<>();
	private Map<Pair<String, UUID>, List<JRStyle>> reportStyles = new ConcurrentHashMap<>();
	//private Map<UUID, String> cachedSubreportsMap = new HashMap<UUID, String>();

	/**
	 * 
	 */
	public static KlexDesignCache getInstance(KlexReportsContext klexReportsContext, ReportContext reportContext)//FIXMECONTEXT a jr context change would be inconsistent
	{
		KlexDesignCache cache = null;

		if (reportContext != null)
		{
			cache = (KlexDesignCache)reportContext.getParameterValue(PARAMETER_KLEX_DESIGN_CACHE);
			
			if (cache == null)
			{
				cache = new KlexDesignCache(klexReportsContext);
				reportContext.setParameterValue(PARAMETER_KLEX_DESIGN_CACHE, cache);
			}
		}
		
		return cache;
	}
	
	public static KlexDesignCache getExistingInstance(ReportContext reportContext)
	{
		KlexDesignCache cache = null;
		if (reportContext != null)
		{
			cache = (KlexDesignCache) reportContext.getParameterValue(PARAMETER_KLEX_DESIGN_CACHE);
		}
		return cache;
	}
	
	/**
	 * 
	 */
	private KlexDesignCache(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
		this.reportCompiler = new DefaultReportCompiler(klexReportsContext);
	}
	
	/**
	 * 
	 */
	public KlexReport getKlexReport(String uri)
	{
		KlexDesignReportResource resource = getResource(uri);
		if (resource != null)
		{
			return resource.getReport();
		}
		return null;
	}

	/**
	 * 
	 */
	public KlexDesign getKlexDesign(String uri)
	{
		return getKlexDesign(uri, true);
	}

	public KlexDesign getKlexDesign(String uri, boolean markDirty)
	{
		KlexDesignReportResource resource = getResource(uri);
		if (resource != null)
		{
			ensureKlexDesign(resource, markDirty);
			return resource.getKlexDesign();
		}
		return null;
	}

	/**
	 * 
	 */
	public void set(String uri, KlexReport klexReport)
	{
		KlexDesignReportResource resource = new KlexDesignReportResource();
		resource.setReport(klexReport);
		cachedResourcesMap.put(uri, resource);
	}

	/**
	 * 
	 */
	public void set(String uri, KlexDesign klexDesign)
	{
		KlexDesignReportResource resource = new KlexDesignReportResource();
		resource.setKlexDesign(klexDesign);
		cachedResourcesMap.put(uri, resource);
	}

	/**
	 * 
	 */
	public void resetKlexReport(String uri)
	{
		KlexDesignReportResource resource = cachedResourcesMap.get(uri);
		if (resource != null)
		{
			resource.setReport(null);
		}
		//cachedResourcesMap.put(uri, resource);
	}

	public KlexDesignReportResource remove(String uri)
	{
		return cachedResourcesMap.remove(uri);
	}
	
	public void set(String uri, KlexDesignReportResource resource)
	{
		cachedResourcesMap.put(uri, resource);
	}
	
	public void clear()
	{
		cachedResourcesMap.clear();
	}
	
	/**
	 * 
	 */
	private KlexDesignReportResource getResource(String uri)
	{
		KlexDesignReportResource resource = cachedResourcesMap.get(uri);
		
		if (resource != null)
		{
			KlexDesign klexDesign = resource.getKlexDesign();
			KlexReport klexReport = resource.getReport();
			
			if (klexReport == null && klexDesign != null)
			{
				try
				{
					klexReport = reportCompiler.compile(klexDesign);
					resource.setReport(klexReport);
				}
				catch (JRException e)
				{
					throw new JRRuntimeException(e);
				}
			}
		}
		
		return resource;
	}

	protected void ensureKlexDesign(KlexDesignReportResource resource, boolean markDirty)
	{
		KlexDesign klexDesign = resource.getKlexDesign();
		KlexReport klexReport = resource.getReport();
		if (klexDesign == null)
		{
			if (klexReport == null)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_INVALID_ENTRY,
						new Object[]{"KlexDesignCache"});
			}
			else
			{
				ByteArrayInputStream bais = null;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try
				{
					new JRXmlWriter(klexReportsContext).write(klexReport, baos, "UTF-8");
					bais = new ByteArrayInputStream(baos.toByteArray());
					klexDesign = JRXmlLoader.load(bais);
					resource.setKlexDesign(klexDesign, markDirty);
				}
				catch (JRException e)
				{
					throw new JRRuntimeException(e);
				}
				finally
				{
					try
					{
						baos.close();
						if (bais != null)
						{
							bais.close();
						}
					}
					catch (IOException e)
					{
					}
				}
			}
		}
		else if (markDirty)
		{
			resource.setDesignDirty(true);
		}
	}

	/**
	 * 
	 */
	public Map<String, KlexDesignReportResource> getCachedResources()
	{
		return cachedResourcesMap;
	}

	public List<JRStyle> getStyles(String reportURI, UUID id)
	{
		return reportStyles.get(new Pair<String, UUID>(reportURI, id));
	}

	public void setStyles(String reportURI, UUID id, List<JRStyle> styles)
	{
		if (log.isDebugEnabled())
		{
			log.debug("Setting " + styles.size() + " styles for " + reportURI + " and " + id);
		}

		reportStyles.put(new Pair<>(reportURI, id), styles);
	}
	
	public String locateReport(KlexReport klexReport)
	{
		for (Entry<String, KlexDesignReportResource> reportEntry : cachedResourcesMap.entrySet())
		{
			KlexReport entryReport = reportEntry.getValue().getReport();
			//testing for object identity.
			//should we also check for UUID?  it doesn't seem necessary for now.
			if (entryReport == klexReport) 
			{
				return reportEntry.getKey();
			}
		}
		
		return null;
	}

	public void setReportCompiler(ReportCompiler reportCompiler)
	{
		this.reportCompiler = reportCompiler;
	}
}
