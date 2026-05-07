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
package net.sf.klexreports.components.table;

import java.io.Serializable;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.type.SplitTypeEnum;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * 
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class CompiledRow implements Row, Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private JRExpression printWhenExpression;
	private SplitTypeEnum splitType;

	public CompiledRow()
	{
		super();
	}

	public CompiledRow(Row row, JRBaseObjectFactory factory)
	{
		this.printWhenExpression = factory.getExpression(row.getPrintWhenExpression());
		this.splitType = row.getSplitType();
	}

	@Override
	public JRExpression getPrintWhenExpression()
	{
		return printWhenExpression;
	}

	@Override
	public SplitTypeEnum getSplitType()
	{
		return splitType;
	}

	@Override
	public Object clone() 
	{
		CompiledRow clone = null;

		try
		{
			clone = (CompiledRow)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
		
		clone.printWhenExpression = JRCloneUtils.nullSafeClone(printWhenExpression);
		
		return clone;
	}

}
