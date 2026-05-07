/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Contains classes for report compiling and expressions evaluating.
 * <br/>
 * <h3>Compiling Report Templates</h3>
 * Source report templates, created either by using the API or by parsing JRXML files, are 
 * subject to the report compilation process before they are filled with data. 
 * <p>
 * This is necessary to make various consistency validations and to incorporate into these 
 * report templates data used to evaluate all report expressions at runtime.
 * </p><p> 
 * The compilation process transforms 
 * {@link net.sf.klexreports.engine.design.klexDesign klexDesign} objects into 
 * {@link net.sf.klexreports.engine.klexReport klexReport} objects. Both classes are 
 * implementations of the same basic {@link net.sf.klexreports.engine.JRReport JRReport} 
 * interface. However, {@link net.sf.klexreports.engine.klexReport klexReport} objects cannot be modified once they are produced, 
 * while {@link net.sf.klexreports.engine.design.klexDesign klexDesign} objects can. This is because some modifications made on the 
 * report template would probably require re-validation, or if a report expression is 
 * modified, the compiler-associated data stored inside the report template would have to be 
 * updated. 
 * </p><p>
 * {@link net.sf.klexreports.engine.design.klexDesign klexDesign} objects are produced when parsing JRXML files using the 
 * {@link net.sf.klexreports.engine.xml.JRXmlLoader JRXmlLoader} or created directly by the parent 
 * application if dynamic report templates are required. The GUI tools for editing 
 * klexReports templates also work with this class to make in-memory modifications to 
 * the report templates before storing them on disk. 
 * </p><p>
 * A {@link net.sf.klexreports.engine.design.klexDesign klexDesign} object must be subject to the report compilation process to produce a 
 * {@link net.sf.klexreports.engine.klexReport klexReport} object. 
 * </p><p>
 * Central to this process is the {@link net.sf.klexreports.engine.design.JRCompiler JRCompiler} 
 * interface, which defines two methods, one being the following: 
 * </p>
 * <pre>
 * public klexReport compileReport(klexDesign design) throws JRException;</pre> 
 * There are several implementations for this compiler interface depending on the language 
 * used for the report expressions or the mechanism used for their runtime evaluation. 
 * <p/>
 * <h3>Expressions Scripting Language</h3>
 * The default language for the report expressions is Java, but report expressions
 * can be written in Groovy, JavaScript or any other scripting language as long as a report
 * compiler implementation that can evaluate them at runtime is available.
 * <p>
 * klexReports currently ships report compiler implementations for the Groovy scripting
 * language (<a href="http://groovy.codehaus.org">http://groovy.codehaus.org</a>) and 
 * JavaScript (<a href="http://www.mozilla.org/rhino">http://www.mozilla.org/rhino</a>). 
 * The compiler implementation classes are:
 * <ul>
 * <li>{@link net.sf.klexreports.groovy.JRGroovyCompiler JRGroovyCompiler}</li>
 * <li>{@link net.sf.klexreports.javascript.JavaScriptCompiler JavaScriptCompiler}</li>
 * </ul>
 * Historically, these compiler implementations used to be shipped as separate samples, 
 * but now they are part of the core library.
 * </p>
 * <h3>Related Documentation</h3>
 * <a href="http://community.klexsoft.com/wiki/klexreports-library-tutorial">klexReports Tutorial</a>

*/
package net.sf.klexreports.compilers;
