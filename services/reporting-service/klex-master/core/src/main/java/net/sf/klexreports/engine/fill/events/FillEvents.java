/*
 * KlexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2019 TIBCO Software Inc. All rights reserved.
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
package net.sf.klexreports.engine.fill.events;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import net.sf.klexreports.engine.fill.JRFillContext;
import net.sf.klexreports.engine.fill.events.ReportFillListenerProvider.ListenerConsumer;

/**
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class FillEvents
{

	private final ListenerRegistry<ReportFillEvent> listeners;
	
	public FillEvents(JRFillContext fillContext)
	{
		List<RegisteredListener<ReportFillEvent>> fillListeners = collectFillListeners(fillContext);
		this.listeners = new ListenerRegistry<>(fillListeners);
	}

	private List<RegisteredListener<ReportFillEvent>> collectFillListeners(JRFillContext fillContext)
	{
		List<ReportFillListenerProvider> providers = fillContext.getMasterFiller().getKlexReportsContext()
				.getExtensions(ReportFillListenerProvider.class);
		return providers.stream().flatMap(provider ->
		{
			Builder<RegisteredListener<ReportFillEvent>> builder = Stream.builder();
			provider.produce(fillContext, new ListenerConsumer()
			{
				@SuppressWarnings("unchecked")
				@Override
				public <T extends ReportFillEvent> void accept(Class<T> eventType, ReportEventListener<? super T> listener)
				{
					builder.accept(new RegisteredListener<ReportFillEvent>(eventType, 
							(ReportEventListener<ReportFillEvent>) listener));
				}
			});
			return builder.build();
		}).collect(Collectors.toList());
	}

	public <T extends ReportFillEvent> void triggerEvent(Class<T> eventType, Supplier<T> eventSupplier)
	{
		listeners.triggerEvent(eventType, eventSupplier);
	}
}
