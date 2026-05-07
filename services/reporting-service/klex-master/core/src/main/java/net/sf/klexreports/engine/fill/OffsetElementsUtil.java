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
package net.sf.klexreports.engine.fill;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.base.VirtualizableElementList;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class OffsetElementsUtil
{

	public static void transfer(List<Object> elements, Consumer<JRPrintElement> consumer)
	{
		elements.stream().forEach(item ->
		{
			if (item instanceof JRPrintElement)
			{
				consumer.accept((JRPrintElement) item);
			}
			else
			{
				OffsetElements offsetElements = (OffsetElements) item;
				Consumer<JRPrintElement> offsetElementConsumer = (element -> 
				{
					element.setX(offsetElements.getOffsetX() + element.getX());
					element.setY(offsetElements.getOffsetY() + element.getY());
				});
				offsetElementConsumer = offsetElementConsumer.andThen(consumer);
				
				Collection<? extends JRPrintElement> subElements = offsetElements.getElements();
				if (subElements instanceof VirtualizableElementList)
				{
					((VirtualizableElementList) subElements).transferElements(offsetElementConsumer);
				}
				else
				{
					subElements.stream().forEach(offsetElementConsumer);
					subElements.clear();
				}
			}
		});
	}

}
