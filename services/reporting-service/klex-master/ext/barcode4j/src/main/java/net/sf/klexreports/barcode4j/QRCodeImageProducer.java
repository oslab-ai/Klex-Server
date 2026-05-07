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
package net.sf.klexreports.barcode4j;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.properties.PropertyConstants;
import net.sf.klexreports.renderers.Renderable;

/**
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public interface QRCodeImageProducer
{

	@Property(
			name = "net.sf.klexreports.components.barcode4j.qrcode.producer.{alias}",
			category = PropertyConstants.CATEGORY_BARCODE,
			valueType = Class.class,
			scopes = {PropertyScope.CONTEXT, PropertyScope.REPORT, PropertyScope.COMPONENT},
			scopeQualifications = {QRCodeComponent.COMPONENT_DESIGNATION},
			sinceVersion = PropertyConstants.VERSION_6_0_2
			)
	String PROPERTY_PREFIX_QRCODE_PRODUCER = 
		BarcodeComponent.PROPERTY_PREFIX + "qrcode.producer.";
	
	Renderable createImage(
		KlexReportsContext klexReportsContext,
		JRComponentElement componentElement, 
		QRCodeBean qrCodeBean, 
		String message
		);
	
}
