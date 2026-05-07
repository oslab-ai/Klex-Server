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
package net.sf.klexreports.engine.component;

import net.sf.klexreports.engine.Deduplicable;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.fill.JRFillCloneFactory;
import net.sf.klexreports.engine.fill.JRTemplateElement;
import net.sf.klexreports.engine.fill.PrintElementOriginator;


/**
 * A base abstract implementation of a fill component.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class BaseFillComponent implements FillComponent
{

	/**
	 * The fill context, as set by {@link #initialize(FillContext)}.
	 */
	protected FillContext fillContext;
	protected PrintElementOriginator printElementOriginator;
	
	protected BaseFillComponent()
	{
		//NOP
	}
	
	protected BaseFillComponent(BaseFillComponent fillComponent, JRFillCloneFactory factory)
	{
		//NOP
	}
	
	@Override
	public void initialize(FillContext fillContext)
	{
		this.fillContext = fillContext;
		
		this.printElementOriginator = fillContext.getPrintElementOriginator();
	}

	/**
	 * Evaluates an expression.
	 * 
	 * @param expression the expression to evaluate
	 * @param evaluation the evaluation type
	 * @return the evaluation result
	 * @throws JRException
	 */
	protected final Object evaluateExpression(JRExpression expression, byte evaluation) throws JRException
	{
		return fillContext.evaluate(expression, evaluation);
	}

	/**
	 * The default implementation throws {@link UnsupportedOperationException}.
	 * 
	 * <p>
	 * If a component supports delayed evaluation, it needs to override this
	 * method.
	 */
	@Override
	public void evaluateDelayedElement(JRPrintElement element, byte evaluation)
			throws JRException
	{
		throw new UnsupportedOperationException("");
	}

	/**
	 * The default implementation is empty.
	 * 
	 * <p>
	 * Override this method if something needs to be done on component rewind.
	 */
	@Override
	public void rewind()
	{
		// NOOP
	}
	
	/**
	 * Deduplicates an object by looking for an identical one that's already
	 * present in the fill context.
	 * 
	 * This operations should be performed on {@link JRTemplateElement element templates}
	 * in order to prevent creation of a large number of identical instances.
	 * 
	 * @param object the object to deduplicate
	 * @return a duplicate of the object if found, or the passed object if not
	 */
	protected <T extends Deduplicable> T deduplicate(T object)
	{
		return fillContext.getFiller().getFillContext().deduplicate(object);
	}
}
