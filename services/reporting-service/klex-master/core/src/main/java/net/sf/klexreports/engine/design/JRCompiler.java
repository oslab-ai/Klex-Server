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
package net.sf.klexreports.engine.design;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.crosstabs.JRCrosstab;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.KlexReport;
import net.sf.klexreports.engine.fill.JREvaluator;
import net.sf.klexreports.properties.PropertyConstants;


/**
 * This interface exposes methods to load expressions evaluators and compile report templates.
 * <p>
 * Source report templates, created either by using the API or by parsing JRXML files, are
 * subject to the report compilation process before they are filled with data.
 * This is necessary to make various consistency validations and to incorporate into these
 * report templates data used to evaluate all report expressions at runtime.
 * </p><p>
 * The compilation process transforms
 * {@link net.sf.klexreports.engine.design.KlexDesign} objects into
 * {@link net.sf.klexreports.engine.KlexReport} objects. Both classes are
 * implementations of the same basic {@link net.sf.klexreports.engine.JRReport}
 * interface. However, {@link net.sf.klexreports.engine.KlexReport} objects 
 * cannot be modified once they are produced,
 * while {@link net.sf.klexreports.engine.design.KlexDesign} objects can. 
 * This is because some modifications made on the
 * report template would probably require re-validation, or if a report expression is
 * modified, the compiler-associated data stored inside the report template would have to be
 * updated.
 * </p><p>
 * {@link net.sf.klexreports.engine.design.KlexDesign} objects are produced when parsing JRXML files using the
 * {@link net.sf.klexreports.engine.xml.JRXmlLoader} or created directly by the parent
 * application if dynamic report templates are required. The GUI tools for editing
 * KlexReports templates also work with this class to make in-memory modifications to
 * the report templates before storing them on disk.
 * </p><p>
 * A {@link net.sf.klexreports.engine.design.KlexDesign} object must be 
 * subject to the report compilation process to produce a
 * {@link net.sf.klexreports.engine.KlexReport} object.
 * </p><p>
 * Central to this process is this {@link net.sf.klexreports.engine.design.JRCompiler}
 * interface, which defines few methods, one being the following:
 * </p><p>
 * <code>public KlexReport compileReport(KlexDesign design) throws JRException;</code>
 * </p><p>
 * There are several implementations for this compiler interface depending on the language
 * used for the report expressions or the mechanism used for their runtime evaluation.
 * </p>
 * <h3>Expressions Scripting Language</h3>
 * The default language for the report expressions is Java, but report expressions
 * can be written in Groovy, JavaScript or any other scripting language as long as a report
 * compiler implementation that can evaluate them at runtime is available.
 * <p>
 * KlexReports currently ships report compiler implementations for the Groovy scripting
 * language (<a href="http://groovy.codehaus.org">http://groovy.codehaus.org</a>) and 
 * JavaScript (<a href="http://www.mozilla.org/rhino">http://www.mozilla.org/rhino</a>). </p>
 * The related compiler implementation classes are:
 * <ul>
 * <li>{@link net.sf.klexreports.groovy.JRGroovyCompiler}</li>
 * <li>{@link net.sf.klexreports.javascript.JavaScriptCompiler}</li>
 * </ul>
 * <p>
 * Since the most common scenario is to use the Java language for writing report
 * expressions, default implementations of the report compiler interface are shipped with
 * the library and are ready to use. They generate a Java class from the report expressions
 * and store bytecode in the generated {@link net.sf.klexreports.engine.KlexReport}
 * object for use at report-filling time.</p>
 * <h3>Java Compilers</h3>
 * The Java report compilers come in different flavors depending on the Java compiler used
 * to compile the class that is generated on the fly:
 * <ul>
 * <li>{@link net.sf.klexreports.jdt.JRJdtCompiler}</li>
 * <li>{@link net.sf.klexreports.engine.design.JRJdk13Compiler}</li>
 * <li>{@link net.sf.klexreports.engine.design.JRJavacCompiler}</li>
 * </ul>
 * @see net.sf.klexreports.engine.KlexReport
 * @see net.sf.klexreports.engine.design.KlexDesign
 * @see net.sf.klexreports.engine.xml.JRXmlLoader
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public interface JRCompiler
{

	/**
	 * The name of the class to be used for report compilation.
	 * <p>
	 * No default value.
	 * 
	 * @deprecated Replaced by {@link net.sf.klexreports.engine.design.JRCompiler#COMPILER_PREFIX}.
	 */
	public static final String COMPILER_CLASS = JRPropertiesUtil.PROPERTY_PREFIX + "compiler.class";

	/**
	 * Prefix for properties that map report compilers to expression languages.
	 * <p/>
	 * Properties having this prefix are used to indicate the JRCompiler implementation to be used when compiling
	 * report designs that rely on the expression language specified as property suffix.
	 */
	@Property(
			name = "net.sf.klexreports.compiler.{language}",
			category = PropertyConstants.CATEGORY_COMPILE,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_2_0_1
			)
	public static final String COMPILER_PREFIX = JRPropertiesUtil.PROPERTY_PREFIX + "compiler.";
	
	/**
	 * Whether to keep the java file generated when the report is compiled.
	 * <p>
	 * Defaults to <code>false</code>.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_COMPILE,
			defaultValue = PropertyConstants.BOOLEAN_FALSE,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_1_0_0,
			valueType = Boolean.class
			)
	public static final String COMPILER_KEEP_JAVA_FILE = JRPropertiesUtil.PROPERTY_PREFIX + "compiler.keep.java.file";
	
	/**
	 * The temporary directory used by the report compiler. 
	 * <p>
	 * Defaults to <code>System.getProperty("user.dir")</code>.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_COMPILE,
			defaultValue = "System.getProperty(\"user.dir\")",
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_1_0_0
			)
	public static final String COMPILER_TEMP_DIR = JRPropertiesUtil.PROPERTY_PREFIX + "compiler.temp.dir";
	
	/**
	 * The classpath used by the report compiler. 
	 * <p>
	 * Defaults to <code>System.getProperty("java.class.path")</code>.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_COMPILE,
			scopes = {PropertyScope.CONTEXT},
			defaultValue = "System.getProperty(\"java.class.path\")",
			sinceVersion = PropertyConstants.VERSION_1_0_0
			)
	public static final String COMPILER_CLASSPATH = JRPropertiesUtil.PROPERTY_PREFIX + "compiler.classpath";

	
	/**
	 * Compiles a report design.
	 * <p>
	 * The compilation consists of verification of the design, generation of expression evaluators
	 * and construction of a serializable read-only version of the report.
	 * <p>
	 * A report compiler should usually extend {@link JRAbstractCompiler JRAbstractCompiler}.
	 * 
	 * 
	 * @param klexDesign the report design
	 * @return the compiled report
	 * @throws JRException
	 */
	public KlexReport compileReport(KlexDesign klexDesign) throws JRException;


	/**
	 * Loads the evaluator for a report's main dataset.
	 * 
	 * @param klexReport the report
	 * @return the evaluator for the report's main dataset
	 * @throws JRException
	 */
	public JREvaluator loadEvaluator(KlexReport klexReport) throws JRException;

	
	/**
	 * Loads a expression evaluator class for a dataset of a report.
	 * 
	 * @param klexReport the report
	 * @param dataset the dataset
	 * @return an instance of the dataset evaluator class
	 * @throws JRException
	 */
	public JREvaluator loadEvaluator(KlexReport klexReport, JRDataset dataset) throws JRException;

	
	/**
	 * Loads a expression evaluator class for a crosstab of a report.
	 * 
	 * @param klexReport the report
	 * @param crosstab the crosstab
	 * @return an instance of the dataset evaluator class
	 * @throws JRException
	 */
	public JREvaluator loadEvaluator(KlexReport klexReport, JRCrosstab crosstab) throws JRException;

}
