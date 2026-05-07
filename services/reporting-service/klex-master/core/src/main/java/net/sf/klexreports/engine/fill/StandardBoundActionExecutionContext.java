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

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class StandardBoundActionExecutionContext implements BoundActionExecutionContext
{
	private int currentPageIndex;
	private int totalPages;
	private byte evaluationType;
	private JREvaluationTime evaluationTime;
	
	@Override
	public int getCurrentPageIndex()
	{
		return currentPageIndex;
	}
	
	public void setCurrentPageIndex(int currentPage)
	{
		this.currentPageIndex = currentPage;
	}
	
	@Override
	public int getTotalPages()
	{
		return totalPages;
	}
	
	public void setTotalPages(int totalPages)
	{
		this.totalPages = totalPages;
	}
	
	@Override
	public byte getExpressionEvaluationType()
	{
		return evaluationType;
	}
	
	public void setExpressionEvaluationType(byte evaluationType)
	{
		this.evaluationType = evaluationType;
	}
	
	@Override
	public JREvaluationTime getEvaluationTime()
	{
		return evaluationTime;
	}
	
	public void setEvaluationTime(JREvaluationTime evaluationTime)
	{
		this.evaluationTime = evaluationTime;
	}
}