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
package net.sf.klexreports.engine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sf.klexreports.engine.JRVirtualizable;
import net.sf.klexreports.engine.fill.JRVirtualizationContext;
import net.sf.klexreports.engine.virtualization.VirtualizationInput;
import net.sf.klexreports.engine.virtualization.VirtualizationOutput;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class VirtualizationSerializer
{
	
	public final void writeData(JRVirtualizable o, OutputStream out) throws IOException
	{
		Object virtualData = o.getVirtualData();
		JRVirtualizationContext context = o.getContext();
		writeData(virtualData, context, out);
	}

	public final void writeData(Object virtualData, JRVirtualizationContext context, OutputStream out) throws IOException
	{
		VirtualizationOutput oos = createOutput(context, out);
		oos.writeJRObject(virtualData);
		oos.flush();
	}

	protected abstract VirtualizationOutput createOutput(JRVirtualizationContext context, OutputStream out) 
			throws IOException;
	
	public final void readData(JRVirtualizable o, InputStream in) throws IOException
	{
		Object virtualData = readData(o.getContext(), in);
		o.setVirtualData(virtualData);
	}
	
	public final Object readData(JRVirtualizationContext context, InputStream in) throws IOException
	{
		VirtualizationInput ois = createInput(context, in);
		Object readObject = ois.readJRObject();
		return readObject;
	}

	protected abstract VirtualizationInput createInput(JRVirtualizationContext context, InputStream in) 
			throws IOException;

}
