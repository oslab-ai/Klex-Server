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
package net.sf.klexreports.crosstabs.base;

import java.io.Serializable;

import net.sf.klexreports.crosstabs.JRCrosstabBucket;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.analytics.dataset.BucketOrder;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.util.JRClassLoader;
import net.sf.klexreports.engine.util.JRCloneUtils;


/**
 * Base read-only implementation of {@link net.sf.klexreports.crosstabs.JRCrosstabBucket JRCrosstabBucket}.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRBaseCrosstabBucket implements JRCrosstabBucket, Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public static final String EXCEPTION_MESSAGE_KEY_BUCKET_LOAD_ERROR = "crosstabs.bucket.load.error";

	protected String valueClassName;
	protected String valueClassRealName;
	protected transient Class<?> valueClass;

	protected BucketOrder bucketOrder = BucketOrder.ASCENDING;
	
	protected JRExpression expression;
	protected JRExpression orderByExpression;
	protected JRExpression comparatorExpression;

	protected JRBaseCrosstabBucket()
	{
	}
	
	public JRBaseCrosstabBucket(JRCrosstabBucket bucket, JRBaseObjectFactory factory)
	{
		factory.put(bucket, this);
		
		this.valueClassName = bucket.getValueClassName();
		this.bucketOrder = bucket.getOrder();
		this.expression = factory.getExpression(bucket.getExpression());
		this.orderByExpression = factory.getExpression(bucket.getOrderByExpression());
		this.comparatorExpression = factory.getExpression(bucket.getComparatorExpression());
	}

	@Override
	public String getValueClassName()
	{
		return valueClassName;
	}

	@Override
	public BucketOrder getOrder()
	{
		return bucketOrder;
	}

	@Override
	public JRExpression getExpression()
	{
		return expression;
	}

	@Override
	public JRExpression getOrderByExpression()
	{
		return orderByExpression;
	}

	@Override
	public JRExpression getComparatorExpression()
	{
		return comparatorExpression;
	}
	
	@Override
	public Class<?> getValueClass()
	{
		if (valueClass == null)
		{
			String className = getValueClassRealName();
			if (className != null)
			{
				try
				{
					valueClass = JRClassLoader.loadClassForName(className);
				}
				catch (ClassNotFoundException e)
				{
					throw 
						new JRRuntimeException(
							EXCEPTION_MESSAGE_KEY_BUCKET_LOAD_ERROR,
							(Object[])null,
							e);
				}
			}
		}
		
		return valueClass;
	}

	/**
	 *
	 */
	private String getValueClassRealName()
	{
		if (valueClassRealName == null)
		{
			valueClassRealName = JRClassLoader.getClassRealName(valueClassName);
		}
		
		return valueClassRealName;
	}

	@Override
	public Object clone()
	{
		JRBaseCrosstabBucket clone = null;
		try
		{
			clone = (JRBaseCrosstabBucket) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			// never
			throw new JRRuntimeException(e);
		}
		clone.expression = JRCloneUtils.nullSafeClone(expression);
		clone.orderByExpression = JRCloneUtils.nullSafeClone(orderByExpression);
		clone.comparatorExpression = JRCloneUtils.nullSafeClone(comparatorExpression);
		return clone;
	}
}
