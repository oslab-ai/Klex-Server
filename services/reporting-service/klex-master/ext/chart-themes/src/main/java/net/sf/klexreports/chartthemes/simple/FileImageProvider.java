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
package net.sf.klexreports.chartthemes.simple;

import java.awt.Image;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRImageLoader;
import net.sf.klexreports.repo.RepositoryUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
@JsonTypeName("file")
public class FileImageProvider implements ImageProvider
{
	/**
	 * 
	 */
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 *
	 */
	@JacksonXmlProperty(isAttribute = true)
	private String file;

	
	/**
	 *
	 */
	public FileImageProvider()
	{
	}
	
	
	/**
	 *
	 */
	public FileImageProvider(String file)
	{
		this.file = file;
	}
	
	
	@Override
	public Image getImage(KlexReportsContext klexReportsContext)
	{
		try
		{
			return
				JRImageLoader.getInstance(klexReportsContext).loadAwtImageFromBytes(
					RepositoryUtil.getInstance(klexReportsContext).getBytesFromLocation(file)
					);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}


	public String getFile() {
		return file;
	}


	public void setFile(String file) {
		this.file = file;
	}

}
