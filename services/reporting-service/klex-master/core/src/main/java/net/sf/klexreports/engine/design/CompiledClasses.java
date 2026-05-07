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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import net.sf.klexreports.engine.JRConstants;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class CompiledClasses implements Serializable
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	public static CompiledClasses forClass(String className, byte[] classBytes)
	{
		return new CompiledClasses(Collections.singletonMap(className, classBytes));
	}
	
	private Map<String, byte[]> classes;

	public CompiledClasses()
	{
		super();
	}
	
	public CompiledClasses(Map<String, byte[]> classes)
	{
		super();
		this.classes = classes;
	}

	public byte[] getClassBytes(String className)
	{
		return classes.get(className);
	}
	
}
