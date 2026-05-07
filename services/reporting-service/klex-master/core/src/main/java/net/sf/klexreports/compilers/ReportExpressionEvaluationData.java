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
package net.sf.klexreports.compilers;

import java.io.Serializable;
import java.util.Map;

import net.sf.klexreports.engine.JRConstants;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ReportExpressionEvaluationData implements Serializable
{
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private String compileName;
	private Serializable compileData;
	
	private Map<Integer, DirectExpressionEvaluation> directEvaluations;

	public String getCompileName()
	{
		return compileName;
	}

	public void setCompileName(String compileName)
	{
		this.compileName = compileName;
	}

	public Serializable getCompileData()
	{
		return compileData;
	}

	public void setCompileData(Serializable compileData)
	{
		this.compileData = compileData;
	}

	public Map<Integer, DirectExpressionEvaluation> getDirectEvaluations()
	{
		return directEvaluations;
	}

	public void setDirectEvaluations(Map<Integer, DirectExpressionEvaluation> directEvaluations)
	{
		this.directEvaluations = directEvaluations;
	}
	
}
