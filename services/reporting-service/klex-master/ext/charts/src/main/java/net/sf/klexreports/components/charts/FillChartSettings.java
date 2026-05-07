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
package net.sf.klexreports.components.charts;

import java.awt.Color;

import net.sf.klexreports.charts.JRChart;
import net.sf.klexreports.charts.fill.ChartsFillObjectFactory;
import net.sf.klexreports.charts.type.EdgeEnum;
import net.sf.klexreports.components.spiderchart.SpiderChartCompiler;
import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.JRFont;
import net.sf.klexreports.engine.JRHyperlinkParameter;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.type.HyperlinkTargetEnum;
import net.sf.klexreports.engine.type.HyperlinkTypeEnum;


/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public class FillChartSettings implements ChartSettings
{

	/**
	 *
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	/**
	 *
	 */
	protected ChartSettings parent;

	/**
	 *
	 */
	public FillChartSettings(ChartSettings chartSettings, ChartsFillObjectFactory factory)
	{
		factory.getParent().put(chartSettings, this);
		parent = chartSettings;
	}

	/**
	 * @see net.sf.klexreports.engine.JRAnchor#getAnchorNameExpression()
	 */
	@Override
	public JRExpression getAnchorNameExpression() {
		return parent.getAnchorNameExpression();
	}
	
	/**
	 * @see net.sf.klexreports.engine.JRAnchor#getBookmarkLevelExpression()
	 */
	@Override
	public JRExpression getBookmarkLevelExpression() {
		return parent.getBookmarkLevelExpression();
	}

	/**
	 * @see net.sf.klexreports.engine.JRAnchor#getBookmarkLevel()
	 */
	@Override
	public int getBookmarkLevel() {
		
		return parent.getBookmarkLevel();
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		
		ChartSettings clone = null;
		
		try
		{
			clone = (ChartSettings)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
		return clone;
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getLegendBackgroundColor()
	 */
	@Override
	public Color getLegendBackgroundColor() {
		
		return parent.getLegendBackgroundColor();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getLegendColor()
	 */
	@Override
	public Color getLegendColor() {
		
		return parent.getLegendColor();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getLegendFont()
	 */
	@Override
	public JRFont getLegendFont() {
		
		return parent.getLegendFont();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getLegendPosition()
	 */
	@Override
	public EdgeEnum getLegendPosition() {
		
		return parent.getLegendPosition();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getRenderType()
	 */
	@Override
	public String getRenderType() {
		
		return parent.getRenderType() == null ? JRChart.RENDER_TYPE_DRAW : parent.getRenderType();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getShowLegend()
	 */
	@Override
	public Boolean getShowLegend() {
		
		return parent.getShowLegend();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getSubtitleColor()
	 */
	@Override
	public Color getSubtitleColor() {
		
		return parent.getSubtitleColor();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getSubtitleExpression()
	 */
	@Override
	public JRExpression getSubtitleExpression() {
		
		return parent.getSubtitleExpression();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getSubtitleFont()
	 */
	@Override
	public JRFont getSubtitleFont() {
		
		return parent.getSubtitleFont();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getTitleColor()
	 */
	@Override
	public Color getTitleColor() {
		
		return parent.getTitleColor();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getTitleExpression()
	 */
	@Override
	public JRExpression getTitleExpression() {
		
		return parent.getTitleExpression();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getTitleFont()
	 */
	@Override
	public JRFont getTitleFont() {
		
		return parent.getTitleFont();
	}

	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getTitlePosition()
	 */
	@Override
	public EdgeEnum getTitlePosition() {
		
		return parent.getTitlePosition();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkAnchorExpression()
	 */
	@Override
	public JRExpression getHyperlinkAnchorExpression() {
		
		return parent.getHyperlinkAnchorExpression();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkPageExpression()
	 */
	@Override
	public JRExpression getHyperlinkPageExpression() {
		
		return parent.getHyperlinkPageExpression();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkParameters()
	 */
	@Override
	public JRHyperlinkParameter[] getHyperlinkParameters() {
		
		return parent.getHyperlinkParameters();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkReferenceExpression()
	 */
	@Override
	public JRExpression getHyperlinkReferenceExpression() {
		
		return parent.getHyperlinkReferenceExpression();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkWhenExpression()
	 */
	@Override
	public JRExpression getHyperlinkWhenExpression() {
		
		return parent.getHyperlinkWhenExpression();
	}
	
	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkTarget()
	 */
	@Override
	public HyperlinkTargetEnum getHyperlinkTarget() {
		
		return parent.getHyperlinkTarget();
	}
	
	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkTooltipExpression()
	 */
	@Override
	public JRExpression getHyperlinkTooltipExpression() {
		
		return parent.getHyperlinkTooltipExpression();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getHyperlinkType()
	 */
	@Override
	public HyperlinkTypeEnum getHyperlinkType() {
		
		return parent.getHyperlinkType();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getLinkTarget()
	 */
	@Override
	public String getLinkTarget() {
		
		return parent.getLinkTarget();
	}

	/**
	 * @see net.sf.klexreports.engine.JRHyperlink#getLinkType()
	 */
	@Override
	public String getLinkType() {
		
		return parent.getLinkType();
	}
	
	/**
	 * @see net.sf.klexreports.components.charts.ChartSettings#getLegendColor()
	 */
	@Override
	public Color getBackcolor() {
		
		return parent.getBackcolor();
	}

	@Override
	public String getCustomizerClass()
	{
		return parent.getCustomizerClass();
	}
	
	/**
	 *
	 */
	public void collectExpressions(JRExpressionCollector collector)
	{
		SpiderChartCompiler.collectExpressions(this, collector);
	}

}
