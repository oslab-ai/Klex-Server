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
package net.sf.klexreports.components.iconlabel;

import net.sf.klexreports.components.table.fill.TableReport;
import net.sf.klexreports.engine.JRHyperlink;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRPropertyExpression;
import net.sf.klexreports.engine.JRTextElement;
import net.sf.klexreports.engine.JRTextField;
import net.sf.klexreports.engine.klexReportsContext;
import net.sf.klexreports.engine.design.JRDesignComponentElement;
import net.sf.klexreports.engine.design.JRDesignTextField;
import net.sf.klexreports.engine.type.HorizontalImageAlignEnum;
import net.sf.klexreports.engine.type.HorizontalTextAlignEnum;
import net.sf.klexreports.engine.type.TextAdjustEnum;
import net.sf.klexreports.engine.type.VerticalImageAlignEnum;
import net.sf.klexreports.engine.util.JRBoxUtil;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class IconLabelComponentUtil
{
	private klexReportsContext klexReportsContext;

	/**
	 * 
	 */
	public static IconLabelComponentUtil getInstance(klexReportsContext klexReportsContext)
	{
		return new IconLabelComponentUtil(klexReportsContext);
	}

	/**
	 * 
	 */
	private IconLabelComponentUtil(klexReportsContext klexReportsContext)
	{
		this.klexReportsContext = klexReportsContext;
	}
	
	/**
	 * 
	 */
	public JRDesignComponentElement createIconLabelComponentElement(JRTextElement textElement)
	{
		return createIconLabelComponentElement(textElement, textElement);
	}
	
	/**
	 * 
	 */
	public JRDesignComponentElement createIconLabelComponentElement(JRTextElement parentElement, JRTextElement textElement)
	{
		JRDesignComponentElement componentElement = new JRDesignComponentElement(textElement.getDefaultStyleProvider());
		componentElement.setX(textElement.getX());
		componentElement.setY(textElement.getY());
		componentElement.setHeight(textElement.getHeight());
		componentElement.setWidth(textElement.getWidth());
		componentElement.setStyle(textElement.getStyle());
		componentElement.setStyleNameReference(textElement.getStyleNameReference());
		componentElement.setStyleExpression(textElement.getStyleExpression());
		componentElement.setMode(parentElement.getOwnMode());
		componentElement.setForecolor(parentElement.getOwnForecolor());
		componentElement.setBackcolor(parentElement.getOwnBackcolor());
		componentElement.setStretchType(parentElement.getStretchType());
		componentElement.setPositionType(parentElement.getPositionType());
		componentElement.setKey(parentElement.getKey());
		componentElement.setPrintWhenExpression(parentElement.getPrintWhenExpression());//FIXMEICONLABEL make this and the ones below work
		componentElement.setPrintInFirstWholeBand(parentElement.isPrintInFirstWholeBand());
		componentElement.setPrintRepeatedValues(parentElement.isPrintRepeatedValues());
		componentElement.setPrintWhenDetailOverflows(parentElement.isPrintWhenDetailOverflows());
		componentElement.setPrintWhenGroupChanges(parentElement.getPrintWhenGroupChanges());
		componentElement.setRemoveLineWhenBlank(parentElement.isRemoveLineWhenBlank());
		
		IconLabelComponent iconLabelComponent = new IconLabelComponent(textElement.getDefaultStyleProvider());
		iconLabelComponent.setIconPosition(IconPositionEnum.END);

		if (parentElement.getOwnVerticalTextAlign() != null)
		{
			switch (parentElement.getOwnVerticalTextAlign())
			{
				case BOTTOM :
				{
					iconLabelComponent.setVerticalImageAlign(VerticalImageAlignEnum.BOTTOM);
					break;
				}
				case MIDDLE :
				{
					iconLabelComponent.setVerticalImageAlign(VerticalImageAlignEnum.MIDDLE);
					break;
				}
				case TOP :
				case JUSTIFIED :
				default :
				{
					iconLabelComponent.setVerticalImageAlign(VerticalImageAlignEnum.TOP);
					break;
				}
			}
		}
			
		if (parentElement.getOwnHorizontalTextAlign() != null)
		{
			switch (parentElement.getOwnHorizontalTextAlign())
			{
				case RIGHT :
				{
					iconLabelComponent.setHorizontalImageAlign(HorizontalImageAlignEnum.RIGHT);
					break;
				}
				case CENTER :
				{
					iconLabelComponent.setHorizontalImageAlign(HorizontalImageAlignEnum.CENTER);
					break;
				}
				case LEFT :
				case JUSTIFIED :
				default :
				{
					iconLabelComponent.setHorizontalImageAlign(HorizontalImageAlignEnum.LEFT);
					break;
				}
			}
		}
			
		iconLabelComponent.setLabelFill(ContainerFillEnum.NONE);
		iconLabelComponent.setLineBox(parentElement.getLineBox().clone(iconLabelComponent));
		iconLabelComponent.getLineBox().setPadding((Integer)0);
		iconLabelComponent.getLineBox().setLeftPadding((Integer)0);
		iconLabelComponent.getLineBox().setRightPadding((Integer)0);
		iconLabelComponent.getLineBox().setTopPadding((Integer)0);
		iconLabelComponent.getLineBox().setBottomPadding((Integer)0);
		
		JRDesignTextField labelTextField = new JRDesignTextField(textElement.getDefaultStyleProvider());
		labelTextField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
		labelTextField.setX(0);
		labelTextField.setY(0);
		labelTextField.setWidth(1);
		labelTextField.setHeight(textElement.getHeight());
//				labelTextField.setHeight(Math.max(1, headerTextElement.getHeight() 
//						- headerTextElement.getLineBox().getTopPadding() - headerTextElement.getLineBox().getBottomPadding()));
		labelTextField.setStyle(textElement.getStyle());
		labelTextField.setStyleNameReference(textElement.getStyleNameReference());
		labelTextField.setStyleExpression(textElement.getStyleExpression());
		labelTextField.setMode(parentElement.getOwnMode());
		labelTextField.setFontSize(parentElement.getOwnFontSize());
		labelTextField.setFontName(parentElement.getOwnFontName());
		labelTextField.setForecolor(parentElement.getOwnForecolor());
		labelTextField.setBackcolor(parentElement.getOwnBackcolor());
		labelTextField.setBold(parentElement.isOwnBold());
		labelTextField.setItalic(parentElement.isOwnItalic());
		labelTextField.setUnderline(parentElement.isOwnUnderline());
		labelTextField.setStrikeThrough(parentElement.isOwnStrikeThrough());
		labelTextField.setHorizontalTextAlign(parentElement.getOwnHorizontalTextAlign());
		labelTextField.setVerticalTextAlign(parentElement.getOwnVerticalTextAlign());
		labelTextField.setRotation(parentElement.getOwnRotation());//FIXMEICONLABEL how does it work?
		labelTextField.setMarkup(parentElement.getMarkup());

		JRBoxUtil.copy(parentElement.getLineBox(), labelTextField.getLineBox());
		labelTextField.getLineBox().setRightPadding((Integer)0);
//		labelTextField.getLineBox().setLeftPadding((Integer)0);

		labelTextField.getLineBox().getPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getLeftPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getRightPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getTopPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getBottomPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getLeftPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getRightPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getTopPen().setLineWidth((Float)0f);
		labelTextField.getLineBox().getBottomPen().setLineWidth((Float)0f);

		for(String propName : parentElement.getPropertiesMap().getPropertyNames())
		{
			labelTextField.getPropertiesMap().setProperty(
				propName, 
				parentElement.getPropertiesMap().getProperty(propName)
				);
		}
		
		if (parentElement.getPropertyExpressions() != null)
		{
			for(JRPropertyExpression propExpr : parentElement.getPropertyExpressions())
			{
				labelTextField.addPropertyExpression(propExpr);
			}
		}
		
		JRTextField textField = parentElement instanceof JRTextField ? (JRTextField)parentElement : null;
		if (textField != null)
		{
			labelTextField.setTextAdjust(textField.getTextAdjust());
			labelTextField.setBlankWhenNull(textField.isBlankWhenNull());
			labelTextField.setPattern(textField.getPattern());
			labelTextField.setPatternExpression(textField.getPatternExpression());
			labelTextField.setBookmarkLevel(textField.getBookmarkLevel());
			labelTextField.setAnchorNameExpression(textField.getAnchorNameExpression());
			labelTextField.setBookmarkLevelExpression(textField.getBookmarkLevelExpression());
			labelTextField.setEvaluationTime(textField.getEvaluationTime());
			labelTextField.setEvaluationGroup(textField.getEvaluationGroup());
		}

		JRHyperlink hyperlink = parentElement instanceof JRHyperlink ? (JRHyperlink)parentElement : null;
		if (hyperlink != null)
		{
			labelTextField.setHyperlinkWhenExpression(hyperlink.getHyperlinkWhenExpression());
			labelTextField.setLinkType(hyperlink.getLinkType());
			labelTextField.setHyperlinkAnchorExpression(hyperlink.getHyperlinkAnchorExpression());
			labelTextField.setHyperlinkPageExpression(hyperlink.getHyperlinkPageExpression());
			labelTextField.setHyperlinkReferenceExpression(hyperlink.getHyperlinkReferenceExpression());
			labelTextField.setLinkTarget(hyperlink.getLinkTarget());
			labelTextField.setHyperlinkTooltipExpression(hyperlink.getHyperlinkTooltipExpression());
		}
		
		iconLabelComponent.setLabelTextField(labelTextField);
		
		JRDesignTextField iconTextField = new JRDesignTextField(textElement.getDefaultStyleProvider());
		iconTextField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
		iconTextField.setX(0);
		iconTextField.setY(0);
		iconTextField.setWidth(1);
		iconTextField.setHeight(1);
		iconTextField.setStyle(textElement.getStyle());
		iconTextField.setStyleNameReference(textElement.getStyleNameReference());
		iconTextField.setStyleExpression(textElement.getStyleExpression());
		iconTextField.setMode(parentElement.getOwnMode());
		iconTextField.setFontName(JRPropertiesUtil.getInstance(klexReportsContext).getProperty(TableReport.PROPERTY_ICON_FONT));
		iconTextField.setFontSize(parentElement.getOwnFontSize());
		iconTextField.setForecolor(parentElement.getOwnForecolor());
		iconTextField.setBackcolor(parentElement.getOwnBackcolor());
		iconTextField.setBold(Boolean.FALSE);//parentElement.isOwnBold());
		iconTextField.setItalic(Boolean.FALSE);//parentElement.isOwnItalic());
		iconTextField.setUnderline(Boolean.FALSE);//parentElement.isOwnUnderline());
		iconTextField.setStrikeThrough(Boolean.FALSE);//parentElement.isOwnStrikeThrough());
		iconTextField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
		iconTextField.setVerticalTextAlign(parentElement.getOwnVerticalTextAlign());

		JRBoxUtil.copy(parentElement.getLineBox(), iconTextField.getLineBox());
		iconTextField.getLineBox().setLeftPadding((Integer)0);
//		iconTextField.getLineBox().setRightPadding((Integer)0);

		iconTextField.getLineBox().getPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getLeftPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getRightPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getTopPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getBottomPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getLeftPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getRightPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getTopPen().setLineWidth((Float)0f);
		iconTextField.getLineBox().getBottomPen().setLineWidth((Float)0f);
		
		iconLabelComponent.setIconTextField(iconTextField);
		
		componentElement.setComponent(iconLabelComponent);

		return componentElement;
	}
}
