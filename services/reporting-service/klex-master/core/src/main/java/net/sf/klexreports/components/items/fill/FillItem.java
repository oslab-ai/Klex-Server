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
package net.sf.klexreports.components.items.fill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.klexreports.components.items.Item;
import net.sf.klexreports.components.items.ItemProperty;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.component.FillContextProvider;
import net.sf.klexreports.engine.fill.JRFillExpressionEvaluator;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public abstract class FillItem implements Item
{

	/**
	 *
	 */
	protected Item item;
	protected Map<String, Object> evaluatedProperties;
	protected FillContextProvider fillContextProvider;
	
	/**
	 *
	 */
	public FillItem(
		FillContextProvider	fillContextProvider,
		Item item, 
		JRFillObjectFactory factory
		)
	{
		factory.put(item, this);

		this.item = item;
		this.fillContextProvider = fillContextProvider;
	}
	
	
	/**
	 *
	 */
	public void evaluateProperties(JRFillExpressionEvaluator evaluator, byte evaluation) throws JRException
	{
		List<ItemProperty> itemProperties = getProperties();
		Map<String, Object> result = null;
		if(itemProperties != null && !itemProperties.isEmpty())
		{
			result = new HashMap<>();
			for(ItemProperty property : itemProperties)
			{
				result.put(property.getName(), getEvaluatedValue(property, evaluator, evaluation));
			}
		}
		
		//if some of the item properties are conditioning each other
		verifyValues(result);
		evaluatedProperties = result;
	}


	@Override
	public Object clone() 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ItemProperty> getProperties() 
	{
		return item.getProperties();
	}
	
	public Map<String, Object> getEvaluatedProperties() 
	{
		return evaluatedProperties;
	}

	public Object getEvaluatedValue(ItemProperty property, JRFillExpressionEvaluator evaluator, byte evaluation) throws JRException
	{
		Object result = null;
		if(
			property.getValueExpression() == null 
			|| property.getValueExpression().getText() == null
			|| property.getValueExpression().getText().trim().length() == 0
			)
		{
			result = property.getValue();
		}
		else
		{
			result = evaluator.evaluate(property.getValueExpression(), evaluation);
		}

		verifyValue(property, result);
		
		return result;
	}

	public abstract void verifyValue(ItemProperty property, Object value) throws JRException;
	
	public abstract void verifyValues(Map<String, Object> result) throws JRException;
}
