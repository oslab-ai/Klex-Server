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
package net.sf.klexreports.engine.export.ooxml;

import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.PrintPageFormat;
import net.sf.klexreports.engine.export.ooxml.type.PaperSizeEnum;

/**
 * @author Sanda Zaharia (shertage@users.sourceforge.net)
 */
public final class OoxmlUtils 
{

	public static PaperSizeEnum getSuitablePaperSize(PrintPageFormat pageFormat)
	{
		if (pageFormat != null && pageFormat.getPageWidth() != 0 && pageFormat.getPageHeight() != 0)
		{
			long mmPageWidth = Math.round(((double)pageFormat.getPageWidth() / 72d) * 25.4);
			long mmPageHeight = Math.round(((double)pageFormat.getPageHeight() / 72d) * 25.4);

			for (PaperSizeEnum paperSize : PaperSizeEnum.values())
			{
				if (
					((paperSize.getWidth() == mmPageWidth) && (paperSize.getHeight() == mmPageHeight)) 
					|| ((paperSize.getHeight() == mmPageWidth) && (paperSize.getWidth() == mmPageHeight))
					)
				{
					return paperSize;
				}
			}
		}
		return PaperSizeEnum.UNDEFINED;
	}
	
	public static byte[] sha512(String text, byte[] salt, int spinCount) 
	{
		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance("SHA-512");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new JRRuntimeException(e);
		}
		digest.update(salt);

		byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_16LE));
		byte[] spin = new byte[4]; // int size
		
		try 
		{
			for (int i = 0; i < spinCount; i++)
			{
				spin[0] = (byte) ( ( i        ) & 0xFF );
				spin[1] = (byte) ( ( i >>> 8  ) & 0xFF );
				spin[2] = (byte) ( ( i >>> 16 ) & 0xFF );
				spin[3] = (byte) ( ( i >>> 24 ) & 0xFF );
				
				digest.reset();
				digest.update(hash);
				digest.update(spin);
				digest.digest(hash, 0, hash.length);
			}
		}
		catch (DigestException e)
		{
			throw new JRRuntimeException(e);
		}
		
		return hash;
	}
	
	public static byte[] getSalt() 
	{
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

}
