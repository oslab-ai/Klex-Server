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
package net.sf.klexreports.crosstabs.fill.calculation;

import java.util.List;

import net.sf.klexreports.crosstabs.fill.calculation.BucketDefinition.Bucket;
import net.sf.klexreports.crosstabs.fill.calculation.BucketingService.BucketMap;
import net.sf.klexreports.crosstabs.fill.calculation.MeasureDefinition.MeasureValue;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public interface BucketingData
{

	BucketingServiceContext getServiceContext();

	Bucket getColumnTotalBucket(int columnGroupIndex);

	Bucket getColumnBucket(int columnGroupIndex, Object value);
	
	MeasureValue[] getMeasureTotals(BucketMap bucketMap, Bucket bucket);

	MeasureValue[] getMeasureValues(BucketMap bucketMap, Bucket bucketValue,
			List<Bucket> columnValues);
	
}
