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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import net.sf.klexreports.crosstabs.JRCrosstab;
import net.sf.klexreports.engine.design.JRCompiler;
import net.sf.klexreports.engine.design.JRJavacCompiler;
import net.sf.klexreports.engine.design.JRJdk13Compiler;
import net.sf.klexreports.engine.design.JRValidationFault;
import net.sf.klexreports.engine.design.JRVerifier;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.fill.JREvaluator;
import net.sf.klexreports.engine.fill.KlexReportsContextAware;
import net.sf.klexreports.engine.util.JRClassLoader;
import net.sf.klexreports.engine.util.JRLoader;
import net.sf.klexreports.engine.util.JRSaver;
import net.sf.klexreports.engine.xml.JRXmlLoader;
import net.sf.klexreports.engine.xml.JRXmlWriter;


/**
 * Facade class for compiling report designs into the ready-to-fill form
 * and for getting the XML representation of report design objects for
 * storage or network transfer.
 * <p>
 * This class exposes all the library's report compilation functionality. It has
 * various methods that allow the users to compile JRXML report templates found in files
 * on disk or that come from input streams. It also lets people compile in-memory report
 * templates by directly passing a
 * {@link net.sf.klexreports.engine.design.KlexDesign} object and receiving the
 * corresponding {@link net.sf.klexreports.engine.KlexReport} object.
 * </p><p>
 * Other utility methods include report template verification and JRXML report template
 * generation for in-memory constructed
 * {@link net.sf.klexreports.engine.design.KlexDesign} class instances. These
 * instances are especially useful in GUI tools that simplify report design work.
 * </p><p>
 * The facade class relies on the report template language to determine an appropriate report compiler.
 * The report compilation facade first reads a configuration property called
 * {@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_PREFIX net.sf.klexreports.compiler.&lt;language&gt;} to determine whether a compiler
 * implementation has been configured for the specific report language. If such a property
 * is found, its value is used as compiler implementation class name and the facade
 * instantiates a compiler object and delegates the report compilation to it. By default,
 * KlexReports includes configuration properties that map the Groovy and JavaScript 
 * report compilers to the <code>groovy</code> and <code>javascript</code> 
 * report languages, respectively.
 * </p><p>
 * If the report uses Java as language and no specific compiler has been set for this
 * language, the report compilation facade employs a built-in fall back mechanism that
 * picks the best Java-based report compiler available in the environment in which the
 * report compilation process takes place.
 * </p><p>
 * The facade first reads the
 * configuration property called {@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_CLASS net.sf.klexreports.compiler.class} to allow
 * users to override its built-in compiler-detection logic by providing the name of the report
 * compiler implementation to use directly.
 * </p><p>
 * If the property is not provided, the facade first tries to see if the JDT compiler from the 
 * Eclipse Foundation is available in the application's classpath. If it is, the
 * <code>net.sf.klexreports.jdt.JRJdtCompiler}</code> implementation is used.
 * </p><p>
 * If the JDT compiler is not available, the compilation facade then tries to locate the JDK
 * 1.3-compatible Java compiler from Sun Microsystems. This is normally found in the
 * <code>tools.jar</code> file that comes with the JDK installation.
 * </p><p>
 * If all these fail, the last thing the fall back mechanism does is to try to launch the
 * <code>javac.exe</code> program from the command line in order to compile the temporarily
 * generated Java source file on the fly.
 * </p>
 * <h3>Configuration Properties to Customize Report Compilation</h3>
 * KlexReports offers various mechanisms for letting users
 * customize its behavior. One of these mechanisms is a complete set of configuration
 * properties. The following list contains all the configuration properties that customize
 * report compilation.
 * <dl>
 * <dt>{@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_PREFIX net.sf.klexreports.compiler.&lt;language&gt;}<dt>
 * <dd>Such properties are used for indicating the name of the class that implements the
 * {@link net.sf.klexreports.engine.design.JRCompiler} interface to be instantiated by 
 * the engine for a specific report language
 * when the default compilation is used through this facade class. The
 * value for such a configuration property can be the name of one of the built-in
 * implementations of this interface shipped with the library as listed previously, or the
 * name of a custom-made implementing class.
 * <br/>
 * One can configure report compilers for custom report languages and override the default
 * compiler mappings by setting KlexReports properties of the form
 * {@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_PREFIX net.sf.klexreports.compiler.&lt;language&gt;} to the desired compiler
 * implementation class names. In particular, the mechanism that automatically chooses a
 * Java report compiler can be superseded by explicitly setting the
 * <code>net.sf.klexreports.compiler.java</code> property to the name of one of the built-in
 * Java compiler classes or of a custom compiler implementation class.
 * <br/>
 * Note that the classes implementing the {@link net.sf.klexreports.engine.design.JRCompiler} 
 * interface can also be used directly in
 * the programs without having to call them through this facade class.</dd>
 * <dt>{@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_CLASSPATH net.sf.klexreports.compiler.classpath}<dt>
 * <dd>This property
 * supplies the classpath. JDK-based compilers require that the classpath be
 * supplied as a parameter. They cannot use the current JVM classpath. The supplied
 * classpath resolves class references inside the Java code they are compiling.
 * <br/>
 * This property is not used by the JDT-based report compiler, which simply uses the parent
 * application's classpath during Java source file compilation.</dd>
 * <dt>{@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_TEMP_DIR net.sf.klexreports.compiler.temp.dir}<dt>
 * <dd>The temporary location for the files generated on the fly is by default the current working
 * directory. It can be changed by supplying a value to this
 * configuration property. This is used by
 * the JDT-based compiler only when it is requested that a copy of the on-the-fly generated
 * Java class be kept for debugging purposes as specified by the next configuration
 * property, because normally this report compiler does not work with files on disk.</dd>
 * <dt>{@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_KEEP_JAVA_FILE net.sf.klexreports.compiler.keep.java.file}<dt>
 * <dd>Sometimes, for debugging purposes, it is useful to have the generated <code>*.java</code> file or
 * generated script in order to fix compilation problems related to report expressions. By
 * default, the engine deletes this file after report compilation, along with its corresponding
 * <code>*.class</code> file. To keep it, however, set this configuration property to true.</dd>
 * </dl>
 * <h3>JDT Compiler-Specific Configuration Properties</h3>
 * The JRJdtCompiler report compiler can use special KlexReports configuration
 * properties to configure the underlying JDT Java compiler. This report compiler collects
 * all the KlexReports configuration properties (the ones usually set in the
 * klexreports.properties file) that start with the <code>org.eclipse.jdt.core.</code> prefix
 * and passes them to the JDT Java compiler when compiling the generated Java class to
 * evaluate report expressions.
 * <p>
 * One of the uses of this mechanism is to instruct the JDT compiler to observe Java 1.5
 * code compatibility. To do so, the following properties should be set:</p>
 * <ul>
 * <li><code>org.eclipse.jdt.core.compiler.source=1.5</code></li>
 * <li><code>org.eclipse.jdt.core.compiler.compliance=1.5</code></li>
 * <li><code>org.eclipse.jdt.core.compiler.codegen.TargetPlatform=1.5</code></li>
 * </ul>
 * @see net.sf.klexreports.engine.KlexReport
 * @see net.sf.klexreports.engine.design.KlexDesign
 * @see net.sf.klexreports.engine.design.JRCompiler
 * @see net.sf.klexreports.engine.design.JRVerifier
 * @see net.sf.klexreports.engine.xml.JRXmlLoader
 * @see net.sf.klexreports.engine.xml.JRXmlWriter
 * @see net.sf.klexreports.engine.util.JRLoader
 * @see net.sf.klexreports.engine.util.JRSaver
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public final class KlexCompileManager
{
	public static final String EXCEPTION_MESSAGE_KEY_INSTANTIATE_REPORT_COMPILER_FAILURE = "engine.instantiate.report.compiler.failure";
	public static final String EXCEPTION_MESSAGE_KEY_REPORT_COMPILER_CLASS_NOT_FOUND = "engine.report.compiler.class.not.found";
	public static final String EXCEPTION_MESSAGE_KEY_REPORT_COMPILER_NOT_SET = "engine.report.compiler.not.set";
	
	private KlexReportsContext klexReportsContext;


	/**
	 *
	 */
	private KlexCompileManager(KlexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	
	/**
	 *
	 */
	private static KlexCompileManager getDefaultInstance()
	{
		return new KlexCompileManager(DefaultKlexReportsContext.getInstance());
	}
	
	
	/**
	 *
	 */
	public static KlexCompileManager getInstance(KlexReportsContext klexReportsContext)
	{
		return new KlexCompileManager(klexReportsContext);
	}
	
	
	/**
	 * Compiles the XML report design file specified by the parameter.
	 * The result of this operation is another file that will contain the serialized  
	 * {@link net.sf.klexreports.engine.KlexReport} object representing the compiled report design,
	 * having the same name as the report design as declared in the XML plus the <code>*.klex</code> extension,
	 * located in the same directory as the XML source file.
	 * 
	 * @param sourceFileName XML source file name
	 * @return resulting file name containing a serialized {@link net.sf.klexreports.engine.KlexReport} object 
	 */
	public String compileToFile(String sourceFileName) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		KlexDesign klexDesign = JRXmlLoader.load(sourceFileName);

		File destFile = new File(sourceFile.getParent(), klexDesign.getName() + ".klex");
		String destFileName = destFile.toString();

		compileToFile(klexDesign, destFileName);
		
		return destFileName;
	}


	/**
	 * Compiles the XML report design file received as the first parameter, placing the result 
	 * in the file specified by the second parameter.
	 * The resulting file will contain a serialized instance of a 
	 * {@link net.sf.klexreports.engine.KlexReport} object representing 
	 * the compiled report design. 
	 * 
	 * @param sourceFileName XML source file name
	 * @param destFileName   file name to place the result into
	 */
	public void compileToFile(
		String sourceFileName,
		String destFileName
		) throws JRException
	{
		KlexDesign klexDesign = JRXmlLoader.load(sourceFileName);

		compileToFile(klexDesign, destFileName);
	}


	/**
	 * Compiles the report design object received as the first parameter, placing the result 
	 * in the file specified by the second parameter.
	 * The resulting file will contain a serialized instance of a 
	 * {@link net.sf.klexreports.engine.KlexReport} object representing the compiled report design.
	 * 
	 * @param klexDesign source report design object
	 * @param destFileName file name to place the compiled report design into
	 */
	public void compileToFile(
		KlexDesign klexDesign,
		String destFileName
		) throws JRException
	{
		KlexReport klexReport = compile(klexDesign);

		JRSaver.saveObject(klexReport, destFileName);
	}


	/**
	 * Compiles the XML report design file received as parameter, and returns 
	 * the compiled report design object.
	 *  
	 * @param sourceFileName XML source file name
	 * @return compiled report design object 
	 */
	public  KlexReport compile(String sourceFileName) throws JRException
	{
		KlexDesign klexDesign = JRXmlLoader.load(sourceFileName);

		return compile(klexDesign);
	}


	/**
	 * Compiles the XML representation of the report design read from the supplied input stream and
	 * writes the generated compiled report design object to the output stream specified 
	 * by the second parameter.
	 * 
	 * @param inputStream  XML source input stream
	 * @param outputStream output stream to write the compiled report design to
	 */
	public void compileToStream(
		InputStream inputStream,
		OutputStream outputStream
		) throws JRException
	{
		KlexDesign klexDesign = JRXmlLoader.load(inputStream);

		compileToStream(klexDesign, outputStream);
	}


	/**
	 * Compiles the report design object represented by the first parameter and
	 * writes the generated compiled report design object to the output stream specified 
	 * by the second parameter.
	 * 
	 * @param klexDesign source report design object
	 * @param outputStream output stream to write the compiled report design to
	 */
	public void compileToStream(
		KlexDesign klexDesign,
		OutputStream outputStream
		) throws JRException
	{
		KlexReport klexReport = compile(klexDesign);

		JRSaver.saveObject(klexReport, outputStream);
	}


	/**
	 * Compiles the serialized report design object read from the supplied input stream and
	 * returns the generated compiled report design object.
	 * 
	 * @param inputStream XML source input stream
	 * @return compiled report design object 
	 */
	public KlexReport compile(InputStream inputStream) throws JRException
	{
		KlexDesign klexDesign = JRXmlLoader.load(inputStream);

		return compile(klexDesign);
	}


	/**
	 * Compiles the report design object received as parameter and
	 * returns the generated compiled report design object.
	 *
	 * @param klexDesign source report design object
	 * @return compiled report design object 
	 * @see net.sf.klexreports.engine.design.JRCompiler
	 */
	public KlexReport compile(KlexDesign klexDesign) throws JRException
	{
		return getCompiler(klexDesign).compileReport(klexDesign);
	}


	/**
	 * Verifies the validity and consistency of the report design object.
	 * Returns a collection of {@link JRValidationFault errors}, if problems are found in the report design.
	 *
	 * @param klexDesign report design object to verify
	 * @return collection of {@link JRValidationFault JRValidationFault} if problems are found
	 * @see net.sf.klexreports.engine.design.JRVerifier
	 */
	public Collection<JRValidationFault> verify(KlexDesign klexDesign)
	{
		return JRVerifier.verifyDesign(klexDesign);
	}


	/**
	 * 
	 */
	public JREvaluator getEvaluator(KlexReport klexReport, JRDataset dataset) throws JRException
	{
		JRCompiler compiler = getCompiler(klexReport);
		JREvaluator evaluator = compiler.loadEvaluator(klexReport, dataset);
		initialize(evaluator);
		return evaluator;
	}


	/**
	 * 
	 */
	public JREvaluator getEvaluator(KlexReport klexReport, JRCrosstab crosstab) throws JRException
	{
		JRCompiler compiler = getCompiler(klexReport);
		JREvaluator evaluator = compiler.loadEvaluator(klexReport, crosstab);
		initialize(evaluator);
		return evaluator;
	}


	/**
	 * 
	 */
	public JREvaluator getEvaluator(KlexReport klexReport) throws JRException
	{
		return getEvaluator(klexReport, klexReport.getMainDataset());
	}

	protected void initialize(JREvaluator evaluator)
	{
		if (evaluator instanceof KlexReportsContextAware)
		{
			((KlexReportsContextAware) evaluator).setKlexReportsContext(klexReportsContext);
		}
	}

	
	/**
	 * Generates the XML representation of the report design loaded from the specified filename.
	 * The result of this operation is an "UTF-8" encoded XML file having the same name as 
	 * the report design, plus the <code>*.klex.jrxml</code> extension, located in the same directory as 
	 * the source file.
	 * 
	 * @param sourceFileName source file name containing the report design object
	 * @return XML representation of the report design
	 */
	public String writeToXmlFile(
		String sourceFileName
		) throws JRException
	{
		File sourceFile = new File(sourceFileName);

		/* We need the report name. */
		JRReport report = (JRReport)JRLoader.loadObject(sourceFile);

		File destFile = new File(sourceFile.getParent(), report.getName() + ".klex.jrxml");
		String destFileName = destFile.toString();
		
		writeToXmlFile(report, destFileName);
		
		return destFileName;
	}


	/**
	 * Generates the XML representation of the report design loaded from the first file parameter
	 * and place it in the file specified by the second parameter. The result is "UTF-8" encoded.
	 * 
	 * @param sourceFileName source file name containing the report design object
	 * @param destFileName   output file name to write the XML report design representation to
	 */
	public void writeToXmlFile(
		String sourceFileName, 
		String destFileName
		) throws JRException
	{
		JRReport report = (JRReport)JRLoader.loadObjectFromFile(sourceFileName);

		writeToXmlFile(report, destFileName);
	}

	
	/**
	 * Generates the XML representation of the report design supplied as the first parameter
	 * and place it in the file specified by the second parameter. The result is "UTF-8" encoded.
	 *
	 * @param report       source report design object
	 * @param destFileName output file name to write the XML report design representation to
	 * @see net.sf.klexreports.engine.xml.JRXmlWriter
	 */
	public void writeToXmlFile(
		JRReport report,
		String destFileName
		) throws JRException
	{
		new JRXmlWriter(klexReportsContext).write(
			report,
			destFileName,
			"UTF-8"
			);
	}


	/**
	 * Generates the XML representation of the serialized report design object read from the supplied 
	 * input stream abd writes it to the specified output stream, using the "UTF-8" encoding.
	 * 
	 * @param inputStream  source input stream to read the report design object from
	 * @param outputStream output stream to write the XML report design representation to
	 */
	public void writeToXmlStream(
		InputStream inputStream, 
		OutputStream outputStream
		) throws JRException
	{
		JRReport report = (JRReport)JRLoader.loadObject(inputStream);

		writeToXmlStream(report, outputStream);
	}

	
	/**
	 * Generates the XML representation of the report design object supplied as parameter
	 * and writes it to the specified output stream, using the "UTF-8" encoding.
	 *
	 * @param report       source report design object
	 * @param outputStream output stream to write the XML report design representation to
	 * @see net.sf.klexreports.engine.xml.JRXmlWriter
	 */
	public void writeToXmlStream(
		JRReport report, 
		OutputStream outputStream
		) throws JRException
	{
		new JRXmlWriter(klexReportsContext).write(
			report, 
			outputStream,
			"UTF-8"
			);
	}


	/**
	 * Generates the XML representation of the report design object supplied as parameter
	 * using the "UTF-8" enconding.
	 *
	 * @param report source report design object
	 * @return XML representation of the report design
	 * @see net.sf.klexreports.engine.xml.JRXmlWriter
	 */
	public String writeToXml(JRReport report)
	{
		return new JRXmlWriter(klexReportsContext).write(report, "UTF-8");
	}


	
	
	/**
	 * @see #compileToFile(String)
	 */
	public static String compileReportToFile(String sourceFileName) throws JRException
	{
		return getDefaultInstance().compileToFile(sourceFileName);
	}


	/**
	 * @see #compileToFile(String, String)
	 */
	public static void compileReportToFile(
		String sourceFileName,
		String destFileName
		) throws JRException
	{
		getDefaultInstance().compileToFile(sourceFileName, destFileName);
	}


	/**
	 * @see #compileToFile(KlexDesign, String)
	 */
	public static void compileReportToFile(
		KlexDesign klexDesign,
		String destFileName
		) throws JRException
	{
		getDefaultInstance().compileToFile(klexDesign, destFileName);
	}


	/**
	 * @see #compile(String)
	 */
	public static KlexReport compileReport(String sourceFileName) throws JRException
	{
		return getDefaultInstance().compile(sourceFileName);
	}


	/**
	 * @see #compileToStream(InputStream, OutputStream)
	 */
	public static void compileReportToStream(
		InputStream inputStream,
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().compileToStream(inputStream, outputStream);
	}


	/**
	 * @see #compileToStream(KlexDesign, OutputStream)
	 */
	public static void compileReportToStream(
		KlexDesign klexDesign,
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().compileToStream(klexDesign, outputStream);
	}


	/**
	 * @see #compile(InputStream)
	 */
	public static KlexReport compileReport(InputStream inputStream) throws JRException
	{
		return getDefaultInstance().compile(inputStream);
	}


	/**
	 * @see #compile(KlexDesign)
	 */
	public static KlexReport compileReport(KlexDesign klexDesign) throws JRException
	{
		return getDefaultInstance().compile(klexDesign);
	}


	/**
	 * @see #verify(KlexDesign)
	 */
	public static Collection<JRValidationFault> verifyDesign(KlexDesign klexDesign)
	{
		return getDefaultInstance().verify(klexDesign);
	}


	/**
	 * @see #getEvaluator(KlexReport, JRDataset)
	 */
	public static JREvaluator loadEvaluator(KlexReport klexReport, JRDataset dataset) throws JRException
	{
		return getDefaultInstance().getEvaluator(klexReport, dataset);
	}


	/**
	 * @see #getEvaluator(KlexReport, JRCrosstab)
	 */
	public static JREvaluator loadEvaluator(KlexReport klexReport, JRCrosstab crosstab) throws JRException
	{
		return getDefaultInstance().getEvaluator(klexReport, crosstab);
	}


	/**
	 * @see #getEvaluator(KlexReport)
	 */
	public static JREvaluator loadEvaluator(KlexReport klexReport) throws JRException
	{
		return getDefaultInstance().getEvaluator(klexReport);
	}

	
	/**
	 * @see #writeToXmlFile(String)
	 */
	public static String writeReportToXmlFile(
		String sourceFileName
		) throws JRException
	{
		return getDefaultInstance().writeToXmlFile(sourceFileName);
	}


	/**
	 * @see #writeToXmlFile(String, String)
	 */
	public static void writeReportToXmlFile(
		String sourceFileName, 
		String destFileName
		) throws JRException
	{
		getDefaultInstance().writeToXmlFile(
			sourceFileName, 
			destFileName
			);
	}

	
	/**
	 * @see #writeToXmlFile(JRReport, String)
	 */
	public static void writeReportToXmlFile(
		JRReport report,
		String destFileName
		) throws JRException
	{
		getDefaultInstance().writeToXmlFile(report, destFileName);
	}


	/**
	 * @see #writeToXmlStream(InputStream, OutputStream)
	 */
	public static void writeReportToXmlStream(
		InputStream inputStream, 
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().writeToXmlStream(inputStream, outputStream);
	}

	
	/**
	 * @see #writeToXmlStream(JRReport, OutputStream)
	 */
	public static void writeReportToXmlStream(
		JRReport report, 
		OutputStream outputStream
		) throws JRException
	{
		getDefaultInstance().writeToXmlStream(report, outputStream);
	}


	/**
	 * @see #writeToXml(JRReport)
	 */
	public static String writeReportToXml(JRReport report)
	{
		return getDefaultInstance().writeToXml(report);
	}


	/**
	 *
	 */
	private JRCompiler getJavaCompiler()
	{
		JRCompiler compiler = null;

		try 
		{
			Class clazz = JRClassLoader.loadClassForRealName("net.sf.klexreports.jdt.JRJdtCompiler");
			Constructor<JRCompiler> constructor = clazz.getDeclaredConstructor(KlexReportsContext.class);
			compiler = constructor.newInstance(klexReportsContext);
		}
		catch (Exception e)
		{
		}

		if (compiler == null)
		{
			try 
			{
				JRClassLoader.loadClassForRealName("com.sun.tools.javac.Main");
				compiler = new JRJdk13Compiler(klexReportsContext);
			}
			catch (Exception e)
			{
			}
		}

		if (compiler == null)
		{
			compiler = new JRJavacCompiler(klexReportsContext);
		}
		
		return compiler;
	}


	/**
	 *
	 */
	private JRCompiler getCompiler(KlexReport klexReport) throws JRException
	{
		JRCompiler compiler = null;
		
		String compilerClassName = klexReport.getCompilerClass();

		Class<? extends JRCompiler> compilerClass = null;
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader != null)
		{
			try
			{
				@SuppressWarnings("unchecked")
				Class<? extends JRCompiler> tmpCompilerClass = (Class<? extends JRCompiler>)classLoader.loadClass(compilerClassName);
				compilerClass = tmpCompilerClass;
			}
			catch(ClassNotFoundException e)
			{
			}
		}
		
		if (compilerClass == null)
		{
			classLoader = KlexCompileManager.class.getClassLoader();
			try
			{
				if (classLoader == null)
				{
					@SuppressWarnings("unchecked")
					Class<? extends JRCompiler> tmpCompilerClass = (Class<? extends JRCompiler>)Class.forName(compilerClassName);
					compilerClass = tmpCompilerClass;
				}
				else
				{
					@SuppressWarnings("unchecked")
					Class<? extends JRCompiler> tmpCompilerClass = (Class<? extends JRCompiler>)classLoader.loadClass(compilerClassName);
					compilerClass = tmpCompilerClass;
				}
			}
			catch(ClassNotFoundException e)
			{
				throw 
					new JRException(
						EXCEPTION_MESSAGE_KEY_REPORT_COMPILER_CLASS_NOT_FOUND,
						new Object[] {compilerClassName}, 
						e
						);
			}
		}


		try
		{
			Constructor<? extends JRCompiler>  constructor = compilerClass.getConstructor(KlexReportsContext.class);//FIXMECONTEXT check all constructors like that
			compiler = constructor.newInstance(klexReportsContext);
		}
		catch (Exception e)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_INSTANTIATE_REPORT_COMPILER_FAILURE,
					new Object[] {compilerClassName}, 
					e);
		}
		return compiler;
	}

	


	/**
	 *
	 */
	private JRCompiler getCompiler(KlexDesign klexDesign) throws JRException
	{
		JRCompiler compiler = null;

		String compilerClassName = getCompilerClassProperty();
		if (compilerClassName == null || compilerClassName.trim().length() == 0)
		{
			String language = klexDesign.getLanguage();
			compilerClassName = JRPropertiesUtil.getInstance(klexReportsContext).getProperty(JRCompiler.COMPILER_PREFIX + language);
			if (compilerClassName == null || compilerClassName.trim().length() == 0)
			{
				if (JRReport.LANGUAGE_JAVA.equals(language))
				{
					return getJavaCompiler();
				}
				else
				{
					throw 
						new JRException(
							EXCEPTION_MESSAGE_KEY_REPORT_COMPILER_NOT_SET,
							new Object[]{language});
				}
			}
		}

		try 
		{
			Class<?> clazz = JRClassLoader.loadClassForName(compilerClassName);
			
			Constructor<?> contextConstructor;
			try
			{
				contextConstructor = clazz.getConstructor(KlexReportsContext.class);
			}
			catch (NoSuchMethodException e)
			{
				// no context constructor
				contextConstructor = null;
			}
			
			if (contextConstructor == null)
			{
				// assuming default constructor
				compiler = (JRCompiler) clazz.getDeclaredConstructor().newInstance();
			}
			else
			{
				compiler = (JRCompiler) contextConstructor.newInstance(klexReportsContext);
			}
		}
		catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException 
			| IllegalAccessException |  InstantiationException e)
		{
			throw new JRException(
					EXCEPTION_MESSAGE_KEY_INSTANTIATE_REPORT_COMPILER_FAILURE,
					new Object[] {compilerClassName}, 
					e);
		}
		
		return compiler;
	}

	
	/**
	 *
	 */
	@SuppressWarnings("deprecation")
	private String getCompilerClassProperty()
	{
		return JRPropertiesUtil.getInstance(klexReportsContext).getProperty(JRCompiler.COMPILER_CLASS);
	}
}
