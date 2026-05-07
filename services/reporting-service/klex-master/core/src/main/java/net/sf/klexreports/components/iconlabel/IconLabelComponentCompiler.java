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

import net.sf.klexreports.engine.JRExpressionCollector;
import net.sf.klexreports.engine.base.JRBaseObjectFactory;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.ComponentCompiler;
import net.sf.klexreports.engine.design.JRVerifier;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class IconLabelComponentCompiler implements ComponentCompiler 
{

	@Override
	public void collectExpressions(Component component, JRExpressionCollector collector) 
	{
		IconLabelComponent iconLabelComponent = (IconLabelComponent) component;
		collector.collect(iconLabelComponent.getLabelTextField());
		collector.collect(iconLabelComponent.getIconTextField());
	}
	
	@Override
	public Component toCompiledComponent(Component component, JRBaseObjectFactory baseFactory) 
	{
		IconLabelComponent iconLabelComponent = (IconLabelComponent) component;
		return new IconLabelComponent(iconLabelComponent, baseFactory);
	}

	@Override
	public void verify(Component component, JRVerifier verifier) 
	{
		// TODO
	}
	
}
