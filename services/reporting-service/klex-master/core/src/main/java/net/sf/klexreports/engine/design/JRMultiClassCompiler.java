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

import java.io.File;

import net.sf.klexreports.engine.JRException;

/**
 * Interface implemented by classes able to compile multiple source files.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface JRMultiClassCompiler extends JRClassCompiler
{
	
	/**
	 * Compile a set of source files.
	 * 
	 * @param sourceFiles the source files
	 * @param classpath the classpath to be used when compiling
	 * @return a <code>String</code> containing compile errors
	 * @throws JRException
	 */
	public String compileClasses(File[] sourceFiles, String classpath) throws JRException;

}
