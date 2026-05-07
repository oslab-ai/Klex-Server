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
package net.sf.klexreports.crosstabs.fill;

import java.util.Comparator;

import net.sf.klexreports.crosstabs.fill.calculation.BucketingData;
import net.sf.klexreports.crosstabs.fill.calculation.BucketDefinition.Bucket;
import net.sf.klexreports.crosstabs.fill.calculation.BucketingService.BucketMap;
import net.sf.klexreports.crosstabs.fill.calculation.BucketingServiceContext;
import net.sf.klexreports.crosstabs.fill.calculation.MeasureDefinition.MeasureValue;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRExpression;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BucketExpressionOrderer implements BucketOrderer
{
	private final JRExpression orderByExpression;
	private final Comparator<Object> orderValueComparator;
	
	private BucketingData bucketingData;
	
	public BucketExpressionOrderer(JRExpression orderByExpression, Comparator<Object> orderValueComparator)
	{
		this.orderByExpression = orderByExpression;
		this.orderValueComparator = orderValueComparator;
	}

	@Override
	public void init(BucketingData bucketingData)
	{
		this.bucketingData = bucketingData;
	}

	@Override
	public Object getOrderValue(BucketMap bucketMap, Bucket bucketValue) throws JRException
	{
		MeasureValue[] bucketTotals = bucketingData.getMeasureTotals(bucketMap, bucketValue);
		BucketingServiceContext serviceContext = bucketingData.getServiceContext();
		return serviceContext.evaluateMeasuresExpression(orderByExpression, bucketTotals);
	}
	
	@Override
	public int compareOrderValues(Object value1, Object value2)
	{
		// FIXME lucianc handle nulls
		return orderValueComparator.compare(value1, value2);
	}

}
