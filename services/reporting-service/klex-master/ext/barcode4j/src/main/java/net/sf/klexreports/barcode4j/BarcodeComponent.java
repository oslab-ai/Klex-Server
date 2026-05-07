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
package net.sf.klexreports.barcode4j;

import java.io.Serializable;

import net.sf.klexreports.engine.JRCloneable;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JREvaluation;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.design.events.JRChangeEventsSupport;
import net.sf.klexreports.engine.design.events.JRPropertyChangeSupport;
import net.sf.klexreports.engine.type.EvaluationTimeEnum;
import net.sf.klexreports.engine.util.Designated;
import net.sf.klexreports.engine.util.JRCloneUtils;


/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class BarcodeComponent implements Component, JREvaluation, Serializable, JRCloneable, JRChangeEventsSupport, Designated
{
	
	public static final String COMPONENT_NAME_PREFIX = "barcode4j:";
	
	// used as prefix for specific barcode component properties
	public static final String PROPERTY_PREFIX = 
		JRPropertiesUtil.PROPERTY_PREFIX + "components.barcode4j.";

	public static final String PROPERTY_EVALUATION_TIME = "evaluationTime";
	public static final String PROPERTY_EVALUATION_GROUP = "evaluationGroup";
	public static final String PROPERTY_CODE_EXPRESSION = "codeExpression";
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private transient JRPropertyChangeSupport eventSupport;
	
	private EvaluationTimeEnum evaluationTime;
	private String evaluationGroup;
	
	private JRExpression codeExpression;

	public BarcodeComponent()
	{
	}
	
	@Override
	public EvaluationTimeEnum getEvaluationTime()
	{
		return evaluationTime;
	}

	public void setEvaluationTime(EvaluationTimeEnum evaluationTime)
	{
		Object old = this.evaluationTime;
		this.evaluationTime = evaluationTime;
		getEventSupport().firePropertyChange(PROPERTY_EVALUATION_TIME, 
				old, this.evaluationTime);
	}

	@Override
	public String getEvaluationGroup()
	{
		return evaluationGroup;
	}

	public void setEvaluationGroup(String evaluationGroup)
	{
		Object old = this.evaluationGroup;
		this.evaluationGroup = evaluationGroup;
		getEventSupport().firePropertyChange(PROPERTY_EVALUATION_GROUP, 
				old, this.evaluationGroup);
	}

	public JRExpression getCodeExpression()
	{
		return codeExpression;
	}

	public void setCodeExpression(JRExpression codeExpression)
	{
		Object old = this.codeExpression;
		this.codeExpression = codeExpression;
		getEventSupport().firePropertyChange(PROPERTY_CODE_EXPRESSION, 
				old, this.codeExpression);
	}

	@Override
	public Object clone()
	{
		BarcodeComponent clone = null;
		try
		{
			clone = (BarcodeComponent) super.clone();
		} 
		catch (CloneNotSupportedException e)
		{
			// never
			throw new JRRuntimeException(e);
		}
		clone.codeExpression = JRCloneUtils.nullSafeClone(codeExpression);
		clone.eventSupport = null;
		return clone;
	}
	
	protected BarcodeComponent cloneObject() //FIXMENOW where is this method coming from?
	{
		BarcodeComponent clone = null;
		try
		{
			clone = (BarcodeComponent)super.clone();
			clone.eventSupport = null;
		}
		catch (CloneNotSupportedException e)
		{
			// never
			throw new JRRuntimeException(e);
		}
		return clone;
	}
	
	public abstract void receive(BarcodeVisitor visitor);

	@Override
	public JRPropertyChangeSupport getEventSupport()
	{
		synchronized (this)
		{
			if (eventSupport == null)
			{
				eventSupport = new JRPropertyChangeSupport(this);
			}
		}
		
		return eventSupport;
	}
}
