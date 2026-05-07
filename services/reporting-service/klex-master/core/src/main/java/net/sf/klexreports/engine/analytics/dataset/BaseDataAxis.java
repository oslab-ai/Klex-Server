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
package net.sf.klexreports.engine.analytics.dataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.analytics.data.Axis;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.util.JRCloneUtils;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BaseDataAxis implements DataAxis, Serializable
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	protected Axis axis;
	protected List<DataAxisLevel> levels;
	
	public BaseDataAxis()
	{
		this.levels = new ArrayList<>();
	}
	
	public BaseDataAxis(DataAxis dataAxis, JRBaseObjectFactory factory)
	{
		factory.put(dataAxis, this);
		
		this.axis = dataAxis.getAxis();
		
		List<DataAxisLevel> dataLevels = dataAxis.getLevels();
		this.levels = new ArrayList<>(dataLevels.size());
		for (DataAxisLevel level : dataLevels)
		{
			this.levels.add(factory.getDataAxisLevel(level));
		}
	}

	@Override
	public Axis getAxis()
	{
		return axis;
	}

	@Override
	public List<DataAxisLevel> getLevels()
	{
		return levels;
	}

	@Override
	public Object clone() 
	{
		BaseDataAxis clone = null;
		try
		{
			clone = (BaseDataAxis) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			// never
			throw new JRRuntimeException(e);
		}
		
		clone.levels = JRCloneUtils.cloneList(levels);
		return clone;
	}

}
