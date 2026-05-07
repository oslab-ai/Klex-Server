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
package net.sf.klexreports.olap.mondrian;

import mondrian.olap.Member;
import net.sf.klexreports.olap.result.JROlapMember;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRMondrianMember implements JROlapMember
{

	private final Member member;
	private final JRMondrianMember parent;

	public JRMondrianMember(Member member, JRMondrianFactory factory)
	{
		this.member = member;
		this.parent = factory.createMember(member.getParentMember());
	}
	
	@Override
	public int getDepth()
	{
		return member.getDepth();
	}

	@Override
	public String getName()
	{
		return member.getName();
	}

	@Override
	public JROlapMember getParentMember()
	{
		return parent;
	}

	@Override
	public Object getPropertyValue(String propertyName)
	{
		return member.getPropertyValue(propertyName);
	}

	@Override
	public String getUniqueName()
	{
		return member.getUniqueName();
	}

	@Override
	public Object getMember()
	{
		return member;
	}

}
