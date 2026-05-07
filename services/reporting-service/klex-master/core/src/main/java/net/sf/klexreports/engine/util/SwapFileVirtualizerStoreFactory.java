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

import net.sf.klexreports.engine.fill.JRVirtualizationContext;
import net.sf.klexreports.engine.fill.VirtualizerStore;
import net.sf.klexreports.engine.fill.VirtualizerStoreFactory;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class SwapFileVirtualizerStoreFactory implements VirtualizerStoreFactory
{
	private String directory = System.getProperty("java.io.tmpdir");//default value
	private int blockSize = 4096;//default value
	private int minGrowCount = 20;//default value
	private StreamCompression compression;
	
	@Override
	public VirtualizerStore createStore(JRVirtualizationContext virtualizationContext)
	{
		JRSwapFile swapFile = new JRSwapFile(directory, blockSize, minGrowCount);
		return new SwapFileVirtualizerStore(swapFile, true, compression);
	}

	public String getDirectory()
	{
		return directory;
	}

	public void setDirectory(String directory)
	{
		this.directory = directory;
	}

	public int getBlockSize()
	{
		return blockSize;
	}

	public void setBlockSize(int blockSize)
	{
		this.blockSize = blockSize;
	}

	public int getMinGrowCount()
	{
		return minGrowCount;
	}

	public void setMinGrowCount(int minGrowCount)
	{
		this.minGrowCount = minGrowCount;
	}

	public StreamCompression getCompression()
	{
		return compression;
	}

	public void setCompression(StreamCompression compression)
	{
		this.compression = compression;
	}
}
