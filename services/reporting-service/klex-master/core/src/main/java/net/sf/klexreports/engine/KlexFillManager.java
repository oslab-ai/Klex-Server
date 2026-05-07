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
package net.sf.klexreports.engine;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.fill.JRFiller;
import net.sf.klexreports.engine.fill.KlexReportSource;
import net.sf.klexreports.engine.fill.SimpleKlexReportSource;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.engine.util.JRSaver;
import net.sf.klexreports.properties.PropertyConstants;
import net.sf.klexreports.repo.RepositoryResourceContext;
import net.sf.klexreports.repo.RepositoryUtil;
import net.sf.klexreports.repo.ResourceInfo;
import net.sf.klexreports.repo.SimpleRepositoryResourceContext;


/**
 * Facade class for filling compiled report designs with data from report data sources, 
 * in order to produce page-oriented documents, ready-to-print.
 * <p>
 * It exposes a variety of methods that receive a report template in the form of an object, file,
 * or input stream, and also produces a document in various output forms (object, file, or
 * output stream).
 * <p>
 * All methods receive a Map object that should contain the values for the report parameters.
 * These values are retrieved by the engine using the corresponding report parameter name as the key. 
 * <p>
 * There are two types of method signatures with regards to the data source
 * provided for filling the report:
 * <ul>
 * <li>Methods that receive an instance of the {@link net.sf.klexreports.engine.JRDataSource} interface
 * and use it directly for retrieving report data;
 * <li>Methods that receive an instance of the <code>java.sql.Connection</code> interface and retrieve
 * the report data by executing the report internal SQL query through this JDBC connection and wrapping 
 * the returned <code>java.sql.ResultSet</code> object inside a {@link net.sf.klexreports.engine.JRResultSetDataSource}
 * instance. 
 * </ul>
 * 
 * @see net.sf.klexreports.engine.KlexReport
 * @see net.sf.klexreports.engine.JRDataSource
 * @see net.sf.klexreports.engine.fill.JRFiller
 * @see net.sf.klexreports.engine.KlexPrint
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class KlexFillManager
{
	
	private static final Log log = LogFactory.getLog(KlexFillManager.class);
	
	/**
	 * Property that determines whether resource paths in subreports, style templates and data adapters 
	 * should be interpreted as relative to the master report location.
	 * <br/>
	 * Starting with version 6.6.0, relative paths in subreports, style templates and data adapters are
	 * resolved as relative to the resource that contains them.
	 * Prior to version 6.6.0, relative paths in subreports, style templates and data adapters were 
	 * resolved as relative to the master report resource.
	 * This property can be set to <code>true</code> to restore the pre 6.6.0 functionality.
	 * <br/>
	 * The default value of the property is <code>false</code>.
	 * <br/>
	 * 
	 * @deprecated The property should only be set when upgrading from a version older than 6.6.0 with a repository
	 * that relied on the fact that paths were relative to the master report.
	 * The property might be removed at some point in the future.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_REPOSITORY,
			defaultValue = PropertyConstants.BOOLEAN_FALSE,
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT},
			sinceVersion = PropertyConstants.VERSION_6_6_0,
			valueType = Boolean.class
			)
	@Deprecated
	public static final String PROPERTY_LEGACY_RELATIVE_PATH_ENABLED = JRPropertiesUtil.PROPERTY_PREFIX
			+ "legacy.relative.path.enabled";
	
	private final KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private KlexFillManager(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	private static KlexFillManager getDefaultInstance()
	{
		return new KlexFillManager(DefaultKlexReportsContext.getInstance());
	}
	
	
	/**
	 *
	 */
	public static KlexFillManager getInstance(KlexReportsContext klexReportsContext)
	{
		return new KlexFillManager(klexReportsContext);
	}
	
	
	/**
	 * Fills the compiled report design loaded from the specified file.
	 * The result of this operation is another file that will contain the serialized  
	 * {@link KlexPrint} object representing the generated document,
	 * having the same name as the report design as declared in the source file, 
	 * plus the <code>*.jrprint</code> extension, located in the same directory as the source file. 
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param params     report parameters map
	 * @param connection     JDBC connection object to use for executing the report internal SQL query
	 */
	public String fillToFile(
		String sourceFileName, 
		Map<String,Object> params,
		Connection connection
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		KlexReport klexReport = (KlexReport)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexReport.getName() + ".jrprint");
		String destFileName = destFile.toString();

		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile, klexReport), 
				params, connection);
		
		JRSaver.saveObject(klexPrint, destFileName);
		
		return destFileName;
	}


	/**
	 * Fills the compiled report design loaded from the specified file.
	 * The result of this operation is another file that will contain the serialized  
	 * {@link KlexPrint} object representing the generated document,
	 * having the same name as the report design as declared in the source file, 
	 * plus the <code>*.jrprint</code> extension, located in the same directory as the source file. 
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param params     report parameters map
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public String fillToFile(
		String sourceFileName, 
		Map<String,Object> params
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		KlexReport klexReport = (KlexReport)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexReport.getName() + ".jrprint");
		String destFileName = destFile.toString();

		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile, klexReport), 
				params);

		JRSaver.saveObject(klexPrint, destFileName);

		return destFileName;
	}

	
	/**
	 * Fills the compiled report design loaded from the file received as the first parameter
	 * and places the result in the file specified by the second parameter.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param destFileName   file name to place the generated report into
	 * @param params     report parameters map
	 * @param connection     JDBC connection object to use for executing the report internal SQL query
	 */
	public void fillToFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> params,
		Connection connection
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile), 
				params, connection);
		
		JRSaver.saveObject(klexPrint, destFileName);
	}

	
	/**
	 * Fills the compiled report design loaded from the file received as the first parameter
	 * and places the result in the file specified by the second parameter.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param destFileName   file name to place the generated report into
	 * @param params     report parameters map
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public void fillToFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> params
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile), 
				params);

		JRSaver.saveObject(klexPrint, destFileName);
	}

	
	/**
	 * Fills the compiled report design received as the first parameter
	 * and places the result in the file specified by the second parameter.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param destFileName file name to place the generated report into
	 * @param parameters   report parameters map
	 * @param connection   JDBC connection object to use for executing the report internal SQL query
	 */
	public void fillToFile(
		KlexReport klexReport, 
		String destFileName, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		KlexPrint klexPrint = fill(klexReport, parameters, connection);

		JRSaver.saveObject(klexPrint, destFileName);
	}

	
	/**
	 * Fills the compiled report design received as the first parameter
	 * and places the result in the file specified by the second parameter.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param destFileName file name to place the generated report into
	 * @param parameters   report parameters map
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public void fillToFile(
		KlexReport klexReport, 
		String destFileName, 
		Map<String,Object> parameters
		) throws JRException
	{
		KlexPrint klexPrint = fill(klexReport, parameters);

		JRSaver.saveObject(klexPrint, destFileName);
	}

	
	/**
	 * Fills the compiled report design loaded from the specified file and returns
	 * the generated report object.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param params     report parameters map
	 * @param connection     JDBC connection object to use for executing the report internal SQL query
	 * @return generated report object
	 */
	public KlexPrint fill(
		String sourceFileName, 
		Map<String,Object> params,
		Connection connection
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		return JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile), 
				params, connection);
	}

	
	/**
	 * Fills the compiled report design loaded from the specified file and returns
	 * the generated report object.
	 * 
	 * @param reportLocation the repository location of the compiled report
	 * @param params     report parameters map
	 * @param connection     JDBC connection object to use for executing the report internal SQL query
	 * @return generated report object
	 */
	public KlexPrint fillFromRepo(
		String reportLocation, 
		Map<String,Object> params,
		Connection connection
		) throws JRException
	{
		return JRFiller.fill(klexReportsContext, 
				getReportSource(reportLocation), 
				params, connection);
	}

	
	/**
	 * Fills the compiled report design loaded from the specified file and returns
	 * the generated report object.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param params     report parameters map
	 * @return generated report object
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public KlexPrint fill(
		String sourceFileName, 
		Map<String,Object> params
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		return JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile), 
				params);
	}

	
	/**
	 * Fills the compiled report design loaded from the specified file and returns
	 * the generated report object.
	 * 
	 * @param reportLocation the repository location of the compiled report
	 * @param params     report parameters map
	 * @return generated report object
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public KlexPrint fillFromRepo(
		String reportLocation, 
		Map<String,Object> params
		) throws JRException
	{
		return JRFiller.fill(klexReportsContext, 
				getReportSource(reportLocation), 
				params);
	}

	
	/**
	 * Fills the compiled report design loaded from the supplied input stream and writes
	 * the generated report object to the output stream specified by the second parameter.
	 * 
	 * @param inputStream  input stream to read the compiled report design object from
	 * @param outputStream output stream to write the generated report object to
	 * @param parameters   report parameters map
	 * @param connection   JDBC connection object to use for executing the report internal SQL query
	 */
	public void fillToStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		KlexReport klexReport = (KlexReport)JRLoader.loadObject(inputStream);

		fillToStream(klexReport, outputStream, parameters, connection);
	}

	
	/**
	 * Fills the compiled report design loaded from the supplied input stream and writes
	 * the generated report object to the output stream specified by the second parameter.
	 * 
	 * @param inputStream  input stream to read the compiled report design object from
	 * @param outputStream output stream to write the generated report object to
	 * @param parameters   report parameters map
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public void fillToStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters
		) throws JRException
	{
		KlexReport klexReport = (KlexReport)JRLoader.loadObject(inputStream);

		fillToStream(klexReport, outputStream, parameters);
	}

	
	/**
	 * Fills the compiled report design supplied as the first parameter and writes
	 * the generated report object to the output stream specified by the second parameter.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param outputStream output stream to write the generated report object to
	 * @param parameters   report parameters map
	 * @param connection   JDBC connection object to use for executing the report internal SQL query
	 */
	public void fillToStream(
		KlexReport klexReport, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		KlexPrint klexPrint = fill(klexReport, parameters, connection);

		JRSaver.saveObject(klexPrint, outputStream);
	}

	
	/**
	 * Fills the compiled report design supplied as the first parameter and writes
	 * the generated report object to the output stream specified by the second parameter.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param outputStream output stream to write the generated report object to
	 * @param parameters   report parameters map
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public void fillToStream(
		KlexReport klexReport, 
		OutputStream outputStream, 
		Map<String,Object> parameters
		) throws JRException
	{
		KlexPrint klexPrint = fill(klexReport, parameters);

		JRSaver.saveObject(klexPrint, outputStream);
	}

	
	/**
	 * Fills the compiled report design loaded from the supplied input stream and returns
	 * the generated report object.
	 * 
	 * @param inputStream  input stream to read the compiled report design object from
	 * @param parameters   report parameters map
	 * @param connection   JDBC connection object to use for executing the report internal SQL query
	 * @return generated report object
	 */
	public KlexPrint fill(
		InputStream inputStream, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		KlexReport klexReport = (KlexReport)JRLoader.loadObject(inputStream);

		return fill(klexReport, parameters, connection);
	}

	
	/**
	 * Fills the compiled report design loaded from the supplied input stream and returns
	 * the generated report object.
	 * 
	 * @param inputStream  input stream to read the compiled report design object from
	 * @param parameters   report parameters map
	 * @return generated report object
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public KlexPrint fill(
		InputStream inputStream, 
		Map<String,Object> parameters
		) throws JRException
	{
		KlexReport klexReport = (KlexReport)JRLoader.loadObject(inputStream);

		return fill(klexReport, parameters);
	}

	
	/**
	 * Fills the compiled report design supplied as the first parameter and returns
	 * the generated report object.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param parameters   report parameters map
	 * @param connection   JDBC connection object to use for executing the report internal SQL query
	 * @return generated report object
	 */
	public KlexPrint fill(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		Connection connection
		) throws JRException
	{
		return JRFiller.fill(klexReportsContext, klexReport, parameters, connection);
	}

	
	/**
	 * Fills the compiled report design supplied as the first parameter and returns
	 * the generated report object.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param parameters   report parameters map
	 * @return generated report object
	 * @see JRFiller#fill(KlexReportsContext, KlexReport, Map)
	 */
	public KlexPrint fill(
		KlexReport klexReport, 
		Map<String,Object> parameters 
		) throws JRException
	{
		return JRFiller.fill(klexReportsContext, klexReport, parameters);
	}

	
	/**
	 * Fills the compiled report design loaded from the specified file.
	 * The result of this operation is another file that will contain the serialized  
	 * {@link KlexPrint} object representing the generated document,
	 * having the same name as the report design as declared in the source file, 
	 * plus the <code>*.jrprint</code> extension, located in the same directory as the source file. 
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param params     report parameters map
	 * @param dataSource     data source object
	 */
	public String fillToFile(
		String sourceFileName, 
		Map<String,Object> params,
		JRDataSource dataSource
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		KlexReport klexReport = (KlexReport)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), klexReport.getName() + ".jrprint");
		String destFileName = destFile.toString();

		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile, klexReport), 
				params, dataSource);

		JRSaver.saveObject(klexPrint, destFileName);
		
		return destFileName;
	}

	
	/**
	 * Fills the compiled report design loaded from the file received as the first parameter
	 * and places the result in the file specified by the second parameter.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param destFileName   file name to place the generated report into
	 * @param params     report parameters map
	 * @param dataSource     data source object
	 */
	public void fillToFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> params,
		JRDataSource dataSource
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		KlexPrint klexPrint = JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile), 
				params, dataSource);

		JRSaver.saveObject(klexPrint, destFileName);
	}

	
	/**
	 * Fills the compiled report design received as the first parameter
	 * and places the result in the file specified by the second parameter.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param destFileName file name to place the generated report into
	 * @param parameters   report parameters map
	 * @param dataSource   data source object
	 */
	public void fillToFile(
		KlexReport klexReport, 
		String destFileName, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		KlexPrint klexPrint = fill(klexReport, parameters, dataSource);

		JRSaver.saveObject(klexPrint, destFileName);
	}

	
	/**
	 * Fills the compiled report design loaded from the specified file and returns
	 * the generated report object.
	 * 
	 * @param sourceFileName source file containing the compiled report design
	 * @param params     report parameters map
	 * @param dataSource     data source object
	 * @return generated report object
	 */
	public KlexPrint fill(
		String sourceFileName, 
		Map<String,Object> params,
		JRDataSource dataSource
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		return JRFiller.fill(klexReportsContext, 
				getReportSource(sourceFile), 
				params, dataSource);
	}

	
	/**
	 * Fills the compiled report design loaded from the specified file and returns
	 * the generated report object.
	 * 
	 * @param reportLocation the repository location of the compiled report
	 * @param params     report parameters map
	 * @param dataSource     data source object
	 * @return generated report object
	 */
	public KlexPrint fillFromRepo(
		String reportLocation, 
		Map<String,Object> params,
		JRDataSource dataSource
		) throws JRException
	{
		return JRFiller.fill(klexReportsContext, 
				getReportSource(reportLocation), 
				params, dataSource);
	}

	
	/**
	 * Fills the compiled report design loaded from the supplied input stream and writes
	 * the generated report object to the output stream specified by the second parameter.
	 * 
	 * @param inputStream  input stream to read the compiled report design object from
	 * @param outputStream output stream to write the generated report object to
	 * @param parameters   report parameters map
	 * @param dataSource   data source object
	 */
	public void fillToStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		KlexReport klexReport = (KlexReport)JRLoader.loadObject(inputStream);

		fillToStream(klexReport, outputStream, parameters, dataSource);
	}

	
	/**
	 * Fills the compiled report design supplied as the first parameter and writes
	 * the generated report object to the output stream specified by the second parameter.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param outputStream output stream to write the generated report object to
	 * @param parameters   report parameters map
	 * @param dataSource   data source object
	 */
	public void fillToStream(
		KlexReport klexReport, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		KlexPrint klexPrint = fill(klexReport, parameters, dataSource);

		JRSaver.saveObject(klexPrint, outputStream);
	}

	
	/**
	 * Fills the compiled report design loaded from the supplied input stream and returns
	 * the generated report object.
	 * 
	 * @param inputStream  input stream to read the compiled report design object from
	 * @param parameters   report parameters map
	 * @param dataSource   data source object
	 * @return generated report object
	 */
	public KlexPrint fill(
		InputStream inputStream, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		KlexReport klexReport = (KlexReport)JRLoader.loadObject(inputStream);

		return fill(klexReport, parameters, dataSource);
	}

	
	/**
	 * Fills the compiled report design supplied as the first parameter and returns
	 * the generated report object.
	 * 
	 * @param klexReport compiled report design object to use for filling
	 * @param parameters   report parameters map
	 * @param dataSource   data source object
	 * @return generated report object
	 */
	public KlexPrint fill(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		JRDataSource dataSource
		) throws JRException
	{
		return JRFiller.fill(klexReportsContext, klexReport, parameters, dataSource);
	}
	
	
	/**
	 * @see #fillToFile(String, Map, Connection)
	 */
	public static String fillReportToFile(
		String sourceFileName, 
		Map<String,Object> params,
		Connection connection
		) throws JRException
	{
		return getDefaultInstance().fillToFile(sourceFileName, params, connection);
	}


	/**
	 * @see #fillToFile(String, Map)
	 */
	public static String fillReportToFile(
		String sourceFileName, 
		Map<String,Object> params
		) throws JRException
	{
		return getDefaultInstance().fillToFile(sourceFileName, params);
	}

	
	/**
	 * @see #fillToFile(String, String, Map, Connection)
	 */
	public static void fillReportToFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> params,
		Connection connection
		) throws JRException
	{
		getDefaultInstance().fillToFile(sourceFileName, destFileName, params, connection);
	}

	
	/**
	 * @see #fillToFile(String, String, Map)
	 */
	public static void fillReportToFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> params
		) throws JRException
	{
		getDefaultInstance().fillToFile(sourceFileName, destFileName, params);
	}

	
	/**
	 * @see #fillToFile(KlexReport, String, Map, Connection)
	 */
	public static void fillReportToFile(
		KlexReport klexReport, 
		String destFileName, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		getDefaultInstance().fillToFile(klexReport, destFileName, parameters, connection);
	}

	
	/**
	 * @see #fillToFile(KlexReport, String, Map)
	 */
	public static void fillReportToFile(
		KlexReport klexReport, 
		String destFileName, 
		Map<String,Object> parameters
		) throws JRException
	{
		getDefaultInstance().fillToFile(klexReport, destFileName, parameters);
	}

	
	/**
	 * @see #fill(String, Map, Connection)
	 */
	public static KlexPrint fillReport(
		String sourceFileName, 
		Map<String,Object> params,
		Connection connection
		) throws JRException
	{
		return getDefaultInstance().fill(sourceFileName, params, connection);
	}

	
	/**
	 * @see #fill(String, Map)
	 */
	public static KlexPrint fillReport(
		String sourceFileName, 
		Map<String,Object> params
		) throws JRException
	{
		return getDefaultInstance().fill(sourceFileName, params);
	}

	
	/**
	 * @see #fillToStream(InputStream, OutputStream, Map, Connection)
	 */
	public static void fillReportToStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		getDefaultInstance().fillToStream(inputStream, outputStream, parameters, connection);
	}

	
	/**
	 * @see #fillToStream(InputStream, OutputStream, Map)
	 */
	public static void fillReportToStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters
		) throws JRException
	{
		getDefaultInstance().fillToStream(inputStream, outputStream, parameters);
	}

	
	/**
	 * @see #fillToStream(KlexReport, OutputStream, Map, Connection)
	 */
	public static void fillReportToStream(
		KlexReport klexReport, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		getDefaultInstance().fillToStream(klexReport, outputStream, parameters, connection);
	}

	
	/**
	 * @see #fillToStream(KlexReport, OutputStream, Map)
	 */
	public static void fillReportToStream(
		KlexReport klexReport, 
		OutputStream outputStream, 
		Map<String,Object> parameters
		) throws JRException
	{
		getDefaultInstance().fillToStream(klexReport, outputStream, parameters);
	}

	
	/**
	 * @see #fill(InputStream, Map, Connection)
	 */
	public static KlexPrint fillReport(
		InputStream inputStream, 
		Map<String,Object> parameters,
		Connection connection
		) throws JRException
	{
		return getDefaultInstance().fill(inputStream, parameters, connection);
	}

	
	/**
	 * @see #fill(InputStream, Map)
	 */
	public static KlexPrint fillReport(
		InputStream inputStream, 
		Map<String,Object> parameters
		) throws JRException
	{
		return getDefaultInstance().fill(inputStream, parameters);
	}

	
	/**
	 * @see #fill(KlexReport, Map, Connection)
	 */
	public static KlexPrint fillReport(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		Connection connection
		) throws JRException
	{
		return getDefaultInstance().fill(klexReport, parameters, connection);
	}

	
	/**
	 * @see #fill(KlexReport, Map)
	 */
	public static KlexPrint fillReport(
		KlexReport klexReport, 
		Map<String,Object> parameters 
		) throws JRException
	{
		return getDefaultInstance().fill(klexReport, parameters);
	}

	
	/**
	 * @see #fillToFile(String, Map, JRDataSource)
	 */
	public static String fillReportToFile(
		String sourceFileName, 
		Map<String,Object> params,
		JRDataSource dataSource
		) throws JRException
	{
		return getDefaultInstance().fillToFile(sourceFileName, params, dataSource);
	}

	
	/**
	 * @see #fillToFile(String, String, Map, JRDataSource)
	 */
	public static void fillReportToFile(
		String sourceFileName, 
		String destFileName, 
		Map<String,Object> params,
		JRDataSource dataSource
		) throws JRException
	{
		getDefaultInstance().fillToFile(sourceFileName, destFileName, params, dataSource);
	}

	
	/**
	 * @see #fillToFile(KlexReport, String, Map, JRDataSource)
	 */
	public static void fillReportToFile(
		KlexReport klexReport, 
		String destFileName, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		getDefaultInstance().fillToFile(klexReport, destFileName, parameters, dataSource);
	}

	
	/**
	 * @see #fill(String, Map, JRDataSource)
	 */
	public static KlexPrint fillReport(
		String sourceFileName, 
		Map<String,Object> params,
		JRDataSource dataSource
		) throws JRException
	{
		return getDefaultInstance().fill(sourceFileName, params, dataSource);
	}

	
	/**
	 * @see #fillToStream(InputStream, OutputStream, Map, JRDataSource)
	 */
	public static void fillReportToStream(
		InputStream inputStream, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		getDefaultInstance().fillToStream(inputStream, outputStream, parameters, dataSource);
	}

	
	/**
	 * @see #fillToStream(KlexReport, OutputStream, Map, JRDataSource)
	 */
	public static void fillReportToStream(
		KlexReport klexReport, 
		OutputStream outputStream, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		getDefaultInstance().fillToStream(klexReport, outputStream, parameters, dataSource);
	}

	
	/**
	 * @see #fill(InputStream, Map, JRDataSource)
	 */
	public static KlexPrint fillReport(
		InputStream inputStream, 
		Map<String,Object> parameters,
		JRDataSource dataSource
		) throws JRException
	{
		return getDefaultInstance().fill(inputStream, parameters, dataSource);
	}

	
	/**
	 * @see #fill(KlexReport, Map, JRDataSource)
	 */
	public static KlexPrint fillReport(
		KlexReport klexReport, 
		Map<String,Object> parameters, 
		JRDataSource dataSource
		) throws JRException
	{
		return getDefaultInstance().fill(klexReport, parameters, dataSource);
	}


	protected static KlexReportSource getReportSource(KlexReportsContext klexReportsContext, 
			File reportFile) throws JRException
	{
		KlexFillManager manager = getInstance(klexReportsContext);
		return manager.getReportSource(reportFile);
	}
	
	protected KlexReportSource getReportSource(File reportFile) throws JRException
	{
		KlexReport klexReport = (KlexReport) JRLoader.loadObject(reportFile);
		return getReportSource(reportFile, klexReport);
	}

	protected KlexReportSource getReportSource(File reportFile, KlexReport klexReport)
	{
		//attempting resolve absolute paths as relative, that's what SimpleFileResolver(".") did
		RepositoryResourceContext fallbackContext = SimpleRepositoryResourceContext.of(".");
		SimpleRepositoryResourceContext reportContext = SimpleRepositoryResourceContext.of(
				reportFile.getParent(), fallbackContext);
		
		boolean legacyRelativePath = JRPropertiesUtil.getInstance(klexReportsContext).getBooleanProperty(klexReport, 
				PROPERTY_LEGACY_RELATIVE_PATH_ENABLED, false);
		if (legacyRelativePath)
		{
			//attempt to resolve paths as relative to the master report for backward compatibility
			reportContext.setSelfAsDerivedFallback(true);
		}
		
		return SimpleKlexReportSource.from(klexReport, reportFile.getPath(), reportContext);
	}
	
	protected KlexReportSource getReportSource(String location) throws JRException
	{
		RepositoryUtil repository = RepositoryUtil.getInstance(klexReportsContext);
		ResourceInfo resourceInfo = repository.getResourceInfo(location);
		KlexReportSource source;
		if (resourceInfo == null)
		{
			KlexReport report = repository.getReport(null, location);
			source = SimpleKlexReportSource.from(report, location, null);
		}
		else
		{
			String reportLocation = resourceInfo.getRepositoryResourceLocation();
			String contextLocation = resourceInfo.getRepositoryContextLocation();
			if (log.isDebugEnabled())
			{
				log.debug("report location " + location + " resolved to " + reportLocation
						+ ", context " + contextLocation);
			}
			
			KlexReport report = repository.getReport(null, reportLocation);
			source = SimpleKlexReportSource.from(report, reportLocation, 
					SimpleRepositoryResourceContext.of(contextLocation));
		}
		return source;
	}
}
