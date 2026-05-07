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

import java.util.List;

import net.sf.klexreports.engine.CommonReturnValue;
import net.sf.klexreports.engine.VariableReturnValue;
import net.sf.klexreports.engine.design.JRDesignSubreportReturnValue;
import net.sf.klexreports.engine.type.CalculationEnum;


/**
 * Implementation of {@link net.sf.klexreports.engine.VariableReturnValue VariableReturnValue}
 * used by the filler.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRFillVariableReturnValue extends JRFillCommonReturnValue implements VariableReturnValue
{
	protected final String fromVariable;

	public JRFillVariableReturnValue(
		VariableReturnValue returnValue,
		JRFillObjectFactory factory, 
		BaseReportFiller filler
		)
	{
		super(returnValue, factory, filler);

		fromVariable = returnValue.getFromVariable();
	}

	/**
	 * Cloning constructor.
	 * 
	 * @param fillReturnValue the object to clone
	 * @param factory the clone factory
	 */
	protected JRFillVariableReturnValue(JRFillVariableReturnValue fillReturnValue, JRFillCloneFactory factory)
	{
		super(fillReturnValue, factory);
		
		this.fromVariable = fillReturnValue.fromVariable;
	}

	@Override
	public String getFromVariable()
	{
		return fromVariable;
	}

	@Override
	protected JRFillCommonReturnValue addDerivedReturnValue (
			CommonReturnValue parentReturnValue, 
			List<JRFillCommonReturnValue> returnValueList, 
			JRFillObjectFactory factory, BaseReportFiller filler
			)
	{
		JRFillVariableReturnValue returnValue = factory.getReturnValue((VariableReturnValue)parentReturnValue);
		returnValue.setDerived(true);
		return addReturnValue(returnValue, returnValueList, factory, filler);
	}
	
	@Override
	protected CommonReturnValue createHelperReturnValue(CommonReturnValue returnValue, String nameSuffix, CalculationEnum calculation)
	{
		JRDesignSubreportReturnValue helper = new JRDesignSubreportReturnValue();
		helper.setToVariable(returnValue.getToVariable() + nameSuffix);
		helper.setSubreportVariable(((VariableReturnValue)returnValue).getFromVariable());
		helper.setCalculation(calculation);
		helper.setIncrementerFactoryClassName(helper.getIncrementerFactoryClassName());//FIXMERETURN shouldn't it be returnValue?
		
		return helper;
	}

	@Override
	protected CommonReturnValue createDistinctCountHelperReturnValue(CommonReturnValue returnValue)
	{
		JRDesignSubreportReturnValue helper = new JRDesignSubreportReturnValue();
		helper.setToVariable(returnValue.getToVariable() + "_DISTINCT_COUNT");
		helper.setSubreportVariable(((VariableReturnValue)returnValue).getFromVariable());
		helper.setCalculation(CalculationEnum.NOTHING);
		helper.setIncrementerFactoryClassName(JRDistinctCountIncrementerFactory.class.getName());
		
		return helper;
	}

}
