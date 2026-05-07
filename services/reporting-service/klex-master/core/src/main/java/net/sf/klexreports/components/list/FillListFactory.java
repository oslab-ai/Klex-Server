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
package net.sf.klexreports.components.list;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentFillFactory;
import net.sf.klexreports.engine.component.FillComponent;
import net.sf.klexreports.engine.fill.JRFillCloneFactory;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;
import net.sf.klexreports.engine.type.PrintOrderEnum;

/**
 * Factory of {@link BaseFillList list fill component} instances.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class FillListFactory implements ComponentFillFactory
{

	@Override
	public FillComponent cloneFillComponent(FillComponent component,
			JRFillCloneFactory factory)
	{
		return (FillComponent) ((BaseFillList) component).createClone(factory);
	}

	@Override
	public FillComponent toFillComponent(Component component,
			JRFillObjectFactory factory)
	{
		try
		{
			ListComponent list = (ListComponent) component;
			FillComponent fillList;
			PrintOrderEnum printOrder = list.getPrintOrder();
			if (printOrder == null 
					|| printOrder == PrintOrderEnum.VERTICAL)
			{
				fillList = new VerticalFillList(list, factory);
			}
			else
			{
				fillList = new HorizontalFillList(list, factory);
			}
			return fillList;
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

}
