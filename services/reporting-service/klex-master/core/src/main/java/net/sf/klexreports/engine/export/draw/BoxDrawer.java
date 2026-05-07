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

/*
 * Contributors:
 * Eugene D - eugenedruy@users.sourceforge.net 
 * Adrian Jackson - iapetus@users.sourceforge.net
 * David Taylor - exodussystems@users.sourceforge.net
 * Lars Kristensen - llk@users.sourceforge.net
 */
package net.sf.klexreports.engine.export.draw;

import java.awt.Graphics2D;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRLineBox;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.KlexReportsContext;



/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class BoxDrawer extends ElementDrawer<JRPrintElement>
{
	/**
	 *
	 */
	public BoxDrawer(KlexReportsContext klexReportsContext)
	{
		super(klexReportsContext);
	}
	
	
	@Override
	public void draw(Graphics2D grx, JRPrintElement element, int offsetX, int offsetY) throws JRException
	{
		drawBox(grx, (JRLineBox)element, element, offsetX, offsetY);
	}

}
