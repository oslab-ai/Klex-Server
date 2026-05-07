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

import java.io.ByteArrayOutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.output.svg.SVGCanvasProvider;
import org.w3c.dom.Document;

import net.sf.klexreports.engine.JRComponentElement;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.renderers.Renderable;
import net.sf.klexreports.renderers.SimpleRenderToImageAwareDataRenderer;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class BarcodeSVGImageProducer implements BarcodeImageProducer
{

	@Override
	public Renderable createImage(
		KlexReportsContext klexReportsContext,
		JRComponentElement componentElement,
		BarcodeGenerator barcode, 
		String message
		)
	{
		try
		{
			SVGCanvasProvider provider = 
				new SVGCanvasProvider(
					false, 
					OrientationEnum.getValueOrDefault(
						((Barcode4jComponent)componentElement.getComponent()).getOrientation()
						).getValue()
					);
			barcode.generateBarcode(provider, message);
			Document svgDoc = provider.getDOM();

			Source source = new DOMSource(svgDoc);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Result output = new StreamResult(baos);
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.transform(source, output);

			return SimpleRenderToImageAwareDataRenderer.getInstance(baos.toByteArray());
		}
		catch (Exception e)
		{
			throw new JRRuntimeException(e);
		}
	}

}
