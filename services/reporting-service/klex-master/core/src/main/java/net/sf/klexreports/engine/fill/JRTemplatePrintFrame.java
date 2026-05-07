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

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRLineBox;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPrintElementContainer;
import net.sf.klexreports.engine.JRPrintFrame;
import net.sf.klexreports.engine.PrintElementId;
import net.sf.klexreports.engine.PrintElementVisitor;
import net.sf.klexreports.engine.base.VirtualizableElementList;
import net.sf.klexreports.engine.virtualization.VirtualizationInput;
import net.sf.klexreports.engine.virtualization.VirtualizationOutput;

/**
 * Implementation of {@link net.sf.klexreports.engine.JRPrintFrame JRPrintFrame} that uses
 * {@link net.sf.klexreports.engine.fill.JRTemplateFrame template frames} to store common
 * attributes. 
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class JRTemplatePrintFrame extends JRTemplatePrintElement implements JRPrintFrame, JRPrintElementContainer
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private List<JRPrintElement> elements;
	
	public JRTemplatePrintFrame()
	{
		
	}
	
	/**
	 * Creates a print frame element.
	 * 
	 * @param templateFrame the template frame that the element will use
	 * @param originator
	 */
	public JRTemplatePrintFrame(JRTemplateFrame templateFrame, PrintElementOriginator originator)
	{
		super(templateFrame, originator);
		
		elements = new ArrayList<>();
	}

	protected void setElementsList(List<JRPrintElement> elements)
	{
		this.elements = elements;
	}

	@Override
	public List<JRPrintElement> getElements()
	{
		return elements;
	}

	@Override
	public void addElement(JRPrintElement element)
	{
		elements.add(element);
	}

	public void addElements(Collection<? extends JRPrintElement> elements)
	{
		this.elements.addAll(elements);
	}

	@Override
	public JRLineBox getLineBox()
	{
		return ((JRTemplateFrame)template).getLineBox();
	}
		
	@Override
	public Color getDefaultLineColor() 
	{
		return getForecolor();
	}

	@Override
	public <T> void accept(PrintElementVisitor<T> visitor, T arg)
	{
		visitor.visit(this, arg);
	}

	@Override
	public void writeVirtualized(VirtualizationOutput out) throws IOException
	{
		super.writeVirtualized(out);
		
		if (elements instanceof VirtualizableElementList)
		{
			VirtualizableElementList virtualizableList = ((VirtualizableElementList) elements);
			JRVirtualizationContext virtualizationContext = virtualizableList.getVirtualizationContext();
			//should be already cached by VirtualizableFrame, but it doesn't hurt setting it again
			virtualizationContext.cacheVirtualizableList(PrintElementId.forElement(this), virtualizableList);
			out.writeIntCompressed(-1);
		}
		else
		{
			out.writeIntCompressed(elements.size());
			for (JRPrintElement element : elements)
			{
				// TODO lucianc we only need this when VirtualElementsData has evaluations 
				out.writeJRObject(element, true, false);
			}
		}
	}

	@Override
	public void readVirtualized(VirtualizationInput in) throws IOException
	{
		super.readVirtualized(in);
		
		int size = in.readIntCompressed();
		if (size < 0)
		{
			elements = in.getVirtualizationContext().getVirtualizableList(PrintElementId.forElement(this));
		}
		else
		{
			elements = new ArrayList<>(size);
			for (int i = 0; i < size; i++)
			{
				JRPrintElement element = (JRPrintElement) in.readJRObject();
				elements.add(element);
			}
		}
	}

	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException
	{
		in.defaultReadObject();
		if (elements instanceof VirtualizableElementList)
		{
			VirtualizableElementList virtualizableList = ((VirtualizableElementList) elements);
			JRVirtualizationContext virtualizationContext = virtualizableList.getVirtualizationContext();
			virtualizationContext.cacheVirtualizableList(PrintElementId.forElement(this), virtualizableList);
		}
	}
}
