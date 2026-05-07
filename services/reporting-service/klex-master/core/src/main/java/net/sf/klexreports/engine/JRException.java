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
package net.sf.klexreports.engine;

import java.util.Locale;
import java.util.MissingResourceException;

import net.sf.klexreports.engine.util.MessageProvider;
import net.sf.klexreports.engine.util.MessageUtil;


/**
 * General purpose KlexReports exception.
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRException extends Exception
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	public static final String EXCEPTION_MESSAGES_BUNDLE = "klexreports_messages";
	public static final String EXCEPTION_MESSAGE_KEY_PREFIX = "net.sf.klexreports.exception.";
	
	private String messageKey;
	private Object[] args;


	/**
	 *
	 */
	public JRException(String message)
	{
		super(message);
	}


	/**
	 *
	 */
	public JRException(Throwable t)
	{
		super(t);
	}


	/**
	 *
	 */
	public JRException(String message, Throwable t)
	{
		super(message, t);
	}


	/**
	 *
	 */
	public JRException(String messageKey, Object[] args, Throwable t)
	{
		super(messageKey, t);
		this.messageKey = messageKey;
		this.args = args;
	}


	/**
	 *
	 */
	public JRException(String messageKey, Object... args)
	{
		super(messageKey);
		this.messageKey = messageKey;
		this.args = args;
	}


	/**
	 *
	 */
	public String getMessageKey()
	{
		return messageKey;
	}


	/**
	 *
	 */
	public Object[] getArgs()
	{
		return args;
	}


	@Override
	public String getMessage()
	{
		return getMessage(null, null);
	}


	public String getMessage(KlexReportsContext klexReportsContext, Locale locale)
	{
		if (messageKey != null)
		{
			return 
				resolveMessage(
					klexReportsContext == null ? DefaultKlexReportsContext.getInstance() : klexReportsContext, 
					locale == null ? Locale.getDefault() : locale
					);
		}
		return super.getMessage();
	}


	/**
	 *
	 */
	protected String resolveMessage(KlexReportsContext klexReportsContext, Locale locale)
	{
		if (messageKey != null)
		{
			try
			{
				String bundleName = getMessageBundleName();
				MessageProvider messageProvider = MessageUtil.getInstance(klexReportsContext).getMessageProvider(bundleName);
				return messageProvider.getMessage(getMessageKeyPrefix() + messageKey, args, locale);
			}
			catch (MissingResourceException e)
			{
			}
		}
		return messageKey;
	}

	protected String getMessageBundleName()
	{
		return EXCEPTION_MESSAGES_BUNDLE;
	}
	
	protected String getMessageKeyPrefix()
	{
		return EXCEPTION_MESSAGE_KEY_PREFIX;
	}
}
