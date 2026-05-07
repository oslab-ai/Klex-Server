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

import java.io.IOException;
import java.util.Set;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.virtualization.VirtualizationInput;
import net.sf.klexreports.engine.virtualization.VirtualizationOutput;

/**
 * Print text implementation that supports recorded values.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
//FIXME these objects reach KlexPrints, find another way to store recorded values
public class JRRecordedValuesPrintText extends JRTemplatePrintText implements JRRecordedValuesPrintElement
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private JRRecordedValues recordedValues;

	public JRRecordedValuesPrintText()
	{
		super();
	}
	
	/**
	 * 
	 * @param text
	 * @param originator
	 */
	public JRRecordedValuesPrintText(JRTemplateText text, PrintElementOriginator originator)
	{
		super(text, originator);
	}

	@Override
	public JRRecordedValues getRecordedValues()
	{
		return recordedValues;
	}

	@Override
	public void deleteRecordedValues()
	{
		recordedValues = null;
	}

	@Override
	public void initRecordedValues(Set<JREvaluationTime> evaluationTimes)
	{
		recordedValues = new JRRecordedValues(evaluationTimes);
	}

	@Override
	public void writeVirtualized(VirtualizationOutput out) throws IOException
	{
		super.writeVirtualized(out);
		
		out.writeJRObject(recordedValues);
	}

	@Override
	public void readVirtualized(VirtualizationInput in) throws IOException
	{
		super.readVirtualized(in);
		
		recordedValues = (JRRecordedValues) in.readJRObject();
	}
}
