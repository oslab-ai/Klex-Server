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

import java.util.Map;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.fill.JREvaluator;
import net.sf.klexreports.engine.fill.JRFillField;
import net.sf.klexreports.engine.fill.JRFillParameter;
import net.sf.klexreports.engine.fill.JRFillVariable;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DirectEvaluator extends JREvaluator
{

	@Override
	protected void customizedInit(Map<String, JRFillParameter> parametersMap, Map<String, JRFillField> fieldsMap,
			Map<String, JRFillVariable> variablesMap) throws JRException
	{
		//NOOP
	}

	@Override
	protected Object evaluate(int id) throws Throwable
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object evaluateOld(int id) throws Throwable
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object evaluateEstimated(int id) throws Throwable
	{
		throw new UnsupportedOperationException();
	}

}
