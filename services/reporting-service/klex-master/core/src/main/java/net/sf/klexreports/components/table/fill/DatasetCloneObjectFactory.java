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
package net.sf.klexreports.components.table.fill;

import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;

/**
 * Object factory used to clone a report dataset.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class DatasetCloneObjectFactory extends JRBaseObjectFactory
{

	public static JRDataset cloneDataset(JRDataset dataset)
	{
		DatasetCloneObjectFactory factory = new DatasetCloneObjectFactory();
		return factory.getDataset(dataset);
	}
	
	public DatasetCloneObjectFactory()
	{
		super((JRExpressionCollector) null);
	}
	
	@Override
	public JRExpression getExpression(JRExpression expression,
			boolean assignNotUsedId)
	{
		// same expression objects are used in the cloned dataset
		return expression;
	}
	
}
