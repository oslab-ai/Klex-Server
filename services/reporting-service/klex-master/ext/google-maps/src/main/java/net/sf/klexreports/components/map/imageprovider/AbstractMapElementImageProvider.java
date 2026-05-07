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
package net.sf.klexreports.components.map.imageprovider;

import net.sf.klexreports.components.map.MapComponent;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRGenericPrintElement;
import net.sf.klexreports.engine.JRPrintImage;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.base.JRBasePrintImage;
import net.sf.klexreports.engine.type.HorizontalImageAlignEnum;
import net.sf.klexreports.engine.type.ScaleImageEnum;
import net.sf.klexreports.engine.type.VerticalImageAlignEnum;
import net.sf.klexreports.renderers.Renderable;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public abstract class AbstractMapElementImageProvider implements MapImageProvider {

	abstract protected Renderable createRenderable(KlexReportsContext klexReportsContext, JRGenericPrintElement element) throws JRException;

	@Override
	public JRPrintImage getImage(KlexReportsContext klexReportsContext, JRGenericPrintElement element) throws JRException {
		JRBasePrintImage printImage = createBaseImage(element);

		Renderable renderable = (Renderable) element.getParameterValue(MapComponent.PARAMETER_CACHE_RENDERER);
		if (renderable == null) {
			renderable = createRenderable(klexReportsContext, element);
			element.setParameterValue(MapComponent.PARAMETER_CACHE_RENDERER, renderable);
		}

		printImage.setRenderer(renderable);
		return printImage;
	}

	protected JRBasePrintImage createBaseImage(JRGenericPrintElement element) {
		JRBasePrintImage printImage = new JRBasePrintImage(element.getDefaultStyleProvider());
		printImage.setX(element.getX());
		printImage.setY(element.getY());
		printImage.setWidth(element.getWidth());
		printImage.setHeight(element.getHeight());
		printImage.setStyle(element.getStyle());
		printImage.setMode(element.getMode());
		printImage.setBackcolor(element.getBackcolor());
		printImage.setForecolor(element.getForecolor());

		printImage.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
		printImage.setHorizontalImageAlign(HorizontalImageAlignEnum.LEFT);
		printImage.setVerticalImageAlign(VerticalImageAlignEnum.TOP);

		return printImage;
	}

}
