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
import net.sf.klexreports.engine.KlexReportsContext;

/**
 * Base class for multiple class compilers.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class JRAbstractMultiClassCompiler extends JRAbstractClassCompiler
{
	public static final String EXCEPTION_MESSAGE_KEY_JAVA_SOURCE_COMPILE_ERROR = "compilers.java.source.compile.error";

	/**
	 * 
	 */
	public JRAbstractMultiClassCompiler(KlexReportsContext klexReportsContext)
	{
		super(klexReportsContext);
	}

	@Override
	public String compileClass(File sourceFile, String classpath) throws JRException
	{
		return compileClasses(new File[]{sourceFile}, classpath);
	}

}
