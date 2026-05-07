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
package net.sf.klexreports.interactivity.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRElement;
import net.sf.klexreports.engine.JRElementGroup;
import net.sf.klexreports.engine.JRParameter;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.engine.design.JRDesignComponentElement;
import net.sf.klexreports.engine.design.JRDesignElement;
import net.sf.klexreports.engine.design.KlexDesign;
import net.sf.klexreports.engine.util.JRElementsVisitor;
import net.sf.klexreports.engine.util.MessageProvider;
import net.sf.klexreports.engine.util.MessageUtil;
import net.sf.klexreports.engine.util.UniformElementVisitor;
import net.sf.klexreports.interactivity.commands.CommandStack;
import net.sf.klexreports.interactivity.commands.CommandTarget;
import net.sf.klexreports.repo.KlexDesignCache;
import net.sf.klexreports.repo.KlexDesignReportResource;


/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="actionName")
public abstract class AbstractAction implements Action, Serializable {
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	public static final String PARAM_COMMAND_STACK = "net.sf.klexreports.command.stack";
	public static final String ERR_CONCAT_STRING = "<#_#>";
	
	private KlexReportsContext klexReportsContext;
	private ReportContext reportContext;
	private CommandStack commandStack;
	protected ActionErrors errors;
	
	public AbstractAction(){
	}

	public String getMessagesBundle() {
		return "net.sf.klexreports.web.actions.messages";
	}
	
	public void init(KlexReportsContext klexReportsContext, ReportContext reportContext)//, String reportUri) 
	{
		this.klexReportsContext = klexReportsContext;
		this.reportContext = reportContext;
		commandStack = (CommandStack)reportContext.getParameterValue(PARAM_COMMAND_STACK);
		
		if (commandStack == null) {
			commandStack = new CommandStack();
			reportContext.setParameterValue(PARAM_COMMAND_STACK, commandStack);
		}
		errors = new ActionErrors(klexReportsContext,
				(Locale) reportContext.getParameterValue(JRParameter.REPORT_LOCALE),
				getMessagesBundle());
	}
	
	public KlexReportsContext getKlexReportsContext() {
		return klexReportsContext;
	}
	
	public ReportContext getReportContext() {
		return reportContext;
	}
	
	@Override
	public void run() throws ActionException {
		performAction();
	}
	
	public CommandStack getCommandStack() {
		return commandStack;
	}

	public void setCommandStack(CommandStack commandStack) {
		this.commandStack = commandStack;
	}
	
	
	public abstract void performAction() throws ActionException;


	public static class ActionErrors implements Serializable {
		
		private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

		private KlexReportsContext klexReportsContext;
		private transient MessageProvider messageProvider;
		private Locale locale;
		private String messageBundle;
		private List<String> errorMessages;


		public ActionErrors (KlexReportsContext klexReportsContext, Locale locale, String messageBundle) {
			this.klexReportsContext = klexReportsContext;
			this.locale = locale;
			this.messageBundle = messageBundle;
			this.errorMessages = new ArrayList<>();
		}
		
		public void add(String messageKey, Object... args) {
			errorMessages.add(getMessageProvider().getMessage(messageKey, args, locale));
		}

		public void add(String messageKey) {
			add(messageKey, (Object[])null);
		}

		public void addAndThrow(String messageKey, Object... args) throws ActionException {
			errorMessages.add(getMessageProvider().getMessage(messageKey, args, locale));
			throwAll();
		}
		
		public void addAndThrow(String messageKey) throws ActionException {
			addAndThrow(messageKey, (Object[])null);
		}
		
		public boolean isEmpty() {
			return errorMessages.size() == 0;
		}
		
		public void throwAll() throws ActionException {
			if (!errorMessages.isEmpty()) {
				StringBuilder errBuilder = new StringBuilder();
				for (int i = 0, ln = errorMessages.size(); i < ln; i++) {
					String errMsg = errorMessages.get(i);
					errBuilder.append(errMsg);
					if (i < ln -1) {
						errBuilder.append(ERR_CONCAT_STRING);
					}
				}
				throw new ActionException(errBuilder.toString());
			}	
		}

		private MessageProvider getMessageProvider() {
			if (messageProvider == null) {
				messageProvider = MessageUtil.getInstance(klexReportsContext).getMessageProvider(messageBundle);
			}

			return messageProvider;
		}
	}


	/**
	 * 
	 */
	public CommandTarget getCommandTarget(UUID uuid)
	{
		return getCommandTarget(uuid, JRDesignComponentElement.class, true);
	}

	public CommandTarget getCommandTarget(UUID uuid, boolean markDirty)
	{
		return getCommandTarget(uuid, JRDesignComponentElement.class, markDirty);
	}

	public CommandTarget getCommandTarget(final UUID uuid, final Class<? extends JRDesignElement> elementType)
	{
		return getCommandTarget(uuid, elementType, true);
	}

	public CommandTarget getCommandTarget(final UUID uuid, final Class<? extends JRDesignElement> elementType,
			final boolean markDirty)
	{
		KlexDesignCache cache = KlexDesignCache.getInstance(getKlexReportsContext(), getReportContext());

		Map<String, KlexDesignReportResource> cachedResources = cache.getCachedResources();
		Set<String> uris = cachedResources.keySet();
		for (String uri : uris)
		{
			final CommandTarget target = new CommandTarget();
			target.setUri(uri);
			
			KlexDesign klexDesign = cache.getKlexDesign(uri, false);
			JRElementsVisitor.visitReport(klexDesign, new UniformElementVisitor()
			{
				private boolean found = false;
				
				@Override
				public void visitElementGroup(JRElementGroup elementGroup)
				{
					//NOP
				}
				
				@Override
				protected void visitElement(JRElement element)
				{
					if (!found && elementType.isInstance(element) && uuid.equals(element.getUUID()))
					{
						target.setIdentifiable(element);
						
						// there's no way to stop the graph visit
						found = true;
					}
				}
			});
			
			if (target.getIdentifiable() != null)
			{
				if (markDirty)
				{
					cache.getKlexDesign(target.getUri(), true);
				}

				return target;
			}
		}
		return null;
	}


	@Override
	public boolean requiresRefill() {
		return true;
	}
}
