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
package net.sf.klexreports.pdf.classic;

import java.io.IOException;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.RadioCheckField;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.pdf.common.PdfRadioCheck;
import net.sf.klexreports.pdf.type.PdfFieldCheckTypeEnum;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class ClassicRadioCheck extends ClassicPdfField implements PdfRadioCheck
{

	private RadioCheckField radioCheckField;
	
	public ClassicRadioCheck(ClassicPdfProducer pdfProducer, RadioCheckField radioCheckField)
	{
		super(pdfProducer, radioCheckField);
		this.radioCheckField = radioCheckField;
	}

	public RadioCheckField getRadioCheckField()
	{
		return radioCheckField;
	}
	
	@Override
	public void setCheckType(PdfFieldCheckTypeEnum checkType)
	{
		radioCheckField.setCheckType(checkType.getValue());
	}

	@Override
	public void setOnValue(String value)
	{
		radioCheckField.setOnValue(value);
	}

	@Override
	public void setChecked(boolean checked)
	{
		radioCheckField.setChecked(checked);
	}

	@Override
	public void add()
	{
		try
		{
			PdfFormField ck = radioCheckField.getFullField();
			pdfProducer.getPdfWriter().addAnnotation(ck);
		}
		catch (Exception e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public void addToGroup() throws IOException
	{
		PdfFormField radioGroup = pdfProducer.getRadioGroup(radioCheckField);
		try
		{
			radioGroup.addKid(radioCheckField.getRadioField());
		}
		catch (DocumentException e)
		{
			throw new JRRuntimeException(e);
		}
	}

}
