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
package net.sf.klexreports.olap.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class Tuple
{
	private final List<TupleMember> members;
	
	public Tuple ()
	{
		this.members = new ArrayList<>();
	}
	
	public Tuple (TupleMember member)
	{
		this.members = new ArrayList<>(1);
		addMember(member);
	}
	
	public void addMember (TupleMember member)
	{
		members.add(member);
	}
	
	public List<TupleMember> getMembers ()
	{
		return members;
	}
	
	public String[] getMemberUniqueNames ()
	{
		String[] names = new String[members.size()];
		Iterator<TupleMember> it = members.iterator();
		for (int i = 0; i < names.length; ++i)
		{
			TupleMember member = it.next();
			names[i] = member.getUniqueName();
		}
		return names;
	}
}
