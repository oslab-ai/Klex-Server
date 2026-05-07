/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2022 TIBCO Software Inc. All rights reserved.
 * http://www.klexsoft.com
 *
 * Unless you have purchased a commercial license agreement from klexsoft,
 * the following license terms apply:
 *
 * This program is part of klexReports.
 *
 * klexReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * klexReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with klexReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.klexreports.components.subreport.fill;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.engine.DatasetRunHolder;
import net.sf.klexreports.engine.JRDatasetRun;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRLineBox;
import net.sf.klexreports.engine.JROrigin;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.JRStyle;
import net.sf.klexreports.engine.JRSubreport;
import net.sf.klexreports.engine.klexReport;
import net.sf.klexreports.engine.component.BaseFillComponent;
import net.sf.klexreports.engine.component.Component;
import net.sf.klexreports.engine.component.FillPrepareResult;
import net.sf.klexreports.engine.fill.BuiltinExpressionEvaluatorFactory;
import net.sf.klexreports.engine.fill.JRFillCloneFactory;
import net.sf.klexreports.engine.fill.JRFillComponentElement;
import net.sf.klexreports.engine.fill.JRFillDatasetRun;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;
import net.sf.klexreports.engine.fill.JRTemplateFrame;
import net.sf.klexreports.engine.fill.JRTemplatePrintFrame;
import net.sf.klexreports.engine.fill.VirtualizableFrame;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public abstract class SubreportFillComponent extends BaseFillComponent
{
	private static final Log log = LogFactory.getLog(SubreportFillComponent.class);
	
	private final Component subreportComponent;

	protected final JRFillObjectFactory factory;
	protected ComponentFillSubreport fillSubreport;
	
	private boolean filling;

	protected int fillWidth;
	private Map<JRStyle, JRTemplateFrame> printFrameTemplates = new HashMap<>();

	public SubreportFillComponent(Component subreportComponent, JRFillObjectFactory factory)
	{
		this.subreportComponent = subreportComponent;
		this.factory = factory;
		
		JRDatasetRun datasetRun = getDatasetRun();
		if (datasetRun != null)
		{
			// we need to do this for return values with derived variables
			JRFillDatasetRun fillDatasetRun = factory.getDatasetRun(getDatasetRun());
			// this is needed for returned variables with evaluationTime=Auto
			factory.registerDatasetRun(fillDatasetRun);
		}
	}

	public SubreportFillComponent(SubreportFillComponent subreportComponent, JRFillCloneFactory factory)
	{
		super(subreportComponent, factory);
		
		this.subreportComponent = subreportComponent.subreportComponent;
		this.factory = subreportComponent.factory;
		
		this.printFrameTemplates = subreportComponent.printFrameTemplates;
	}
	
	protected JRDatasetRun getDatasetRun()
	{
		return subreportComponent instanceof DatasetRunHolder ? ((DatasetRunHolder)subreportComponent).getDatasetRun() : null;
	}

	@Override
	public void evaluate(byte evaluation) throws JRException
	{
		if (filling)
		{
			log.warn("Table fill did not complete, canceling previous table subreport");
			fillSubreport.cancelSubreportFill();
		}
		
		filling = false;
		
		if (!isEmpty())
		{
			createFillSubreport();
			fillSubreport.evaluateSubreport(evaluation);
		}
	}

	protected void createFillSubreport() throws JRException
	{
		ComponentFillSubreportFactory subreportFactory = getFillSubreportFactory();
		if (subreportFactory == null)
		{
			subreportFactory = createFillTableSubreportFactory();
			setFillSubreportFactory(subreportFactory);
		}
		
		fillSubreport = subreportFactory.createFillSubreport();
	}
	
	public abstract ComponentFillSubreportFactory getFillSubreportFactory();
	
	public abstract void setFillSubreportFactory(ComponentFillSubreportFactory subreportFactory);

	public abstract klexReport getklexReport(BuiltinExpressionEvaluatorFactory builtinEvaluatorFactory) throws JRException;

	public abstract boolean isEmpty();

	protected ComponentFillSubreportFactory createFillTableSubreportFactory() throws JRException
	{
		BuiltinExpressionEvaluatorFactory builtinEvaluatorFactory = new BuiltinExpressionEvaluatorFactory();
		
		SubreportElementAdapter subreport = 
			new SubreportElementAdapter(
				getDatasetRun(), 
				((JRFillComponentElement)fillContext.getComponentElement()).getParent()
				);
		return 
			new ComponentFillSubreportFactory(
				subreport, 
				getklexReport(builtinEvaluatorFactory),
				//compiledTableReport,
				builtinEvaluatorFactory
				);
	}

	@Override
	public FillPrepareResult prepare(int availableHeight)
	{
		try
		{
			if (isEmpty())
			{
				//no columns to print
				return FillPrepareResult.NO_PRINT_NO_OVERFLOW;
			}
			
			JRTemplatePrintFrame printFrame = new JRTemplatePrintFrame(getFrameTemplate(), printElementOriginator);
			JRLineBox lineBox = printFrame.getLineBox();
			int verticalPadding = lineBox.getTopPadding() + lineBox.getBottomPadding();
			
			FillPrepareResult result = 
				fillSubreport.prepareSubreport(
					availableHeight - verticalPadding, 
					filling
					);
			
			if (verticalPadding != 0)
			{
				result = result.addStretch(verticalPadding);
			}
			
			filling = result.willOverflow();
			return result;
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public JRPrintElement fill()
	{
		JRTemplatePrintFrame printFrame = new JRTemplatePrintFrame(getFrameTemplate(), printElementOriginator);

		JRLineBox lineBox = printFrame.getLineBox();
		
		printFrame.setUUID(fillContext.getComponentElement().getUUID());
		printFrame.setX(fillContext.getComponentElement().getX());
		printFrame.setY(fillContext.getElementPrintY());
		printFrame.setHeight(fillSubreport.getContentsStretchHeight() + lineBox.getTopPadding() + lineBox.getBottomPadding());
		
		List<JRStyle> styles = fillSubreport.getSubreportStyles();
		for (Iterator<JRStyle> it = styles.iterator(); it.hasNext();)
		{
			JRStyle style = it.next();
			try
			{
				fillContext.getFiller().addPrintStyle(style);
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
		}
		
		List<JROrigin> origins = fillSubreport.getSubreportOrigins();
		for (Iterator<JROrigin> it = origins.iterator(); it.hasNext();)
		{
			JROrigin origin = it.next();
			fillContext.getFiller().getklexPrint().addOrigin(origin);
		}
		
		int contentsWidth = fillWidth;
		Collection<JRPrintElement> elements = fillSubreport.getPrintElements();
		if (elements != null)
		{
			VirtualizableFrame virtualizableFrame = new VirtualizableFrame(printFrame, 
					fillContext.getFiller().getVirtualizationContext(), 
					fillContext.getFiller().getCurrentPage());
			
			virtualizableFrame.addOffsetElements(elements, 0, 0);
			virtualizableFrame.fill();
			
			if (fillSubreport.getPrintContentsWidth() > contentsWidth)
			{
				contentsWidth = fillSubreport.getPrintContentsWidth();
			}
		}
		contentsWidth += lineBox.getLeftPadding() + lineBox.getRightPadding();
		
		int elementWidth = fillContext.getComponentElement().getWidth();
		if (contentsWidth < elementWidth)
		{
			contentsWidth = elementWidth; 
		}
		
		printFrame.setWidth(contentsWidth);
		
		fillSubreport.subreportPageFilled();
		
		return printFrame;
	}

	protected JRTemplateFrame getFrameTemplate()
	{
		JRStyle style = fillContext.getElementStyle();
		JRTemplateFrame frameTemplate = printFrameTemplates.get(style);
		if (frameTemplate == null)
		{
			frameTemplate = new JRTemplateFrame(
						fillContext.getElementOrigin(),
						fillContext.getDefaultStyleProvider());
			frameTemplate.setElement(fillContext.getComponentElement());
			frameTemplate = deduplicate(frameTemplate);
			
			printFrameTemplates.put(style, frameTemplate);
		}

		return frameTemplate;
	}

	@Override
	public void rewind()
	{
		if (filling)
		{
			if (log.isDebugEnabled())
			{
				log.debug("Rewinding component subreport");
			}
			
			try
			{
				fillSubreport.rewind();
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
			
			filling = false;
		}
	}

	public class ComponentFillSubreportFactory
	{
		private final JRSubreport subreport;
		private final klexReport klexReport;
		private final BuiltinExpressionEvaluatorFactory builtinEvaluatorFactory;
		
		public ComponentFillSubreportFactory(
			JRSubreport subreport, 
			klexReport klexReport, 
			BuiltinExpressionEvaluatorFactory builtinEvaluatorFactory
			)
		{
			this.subreport = subreport;
			this.klexReport = klexReport;
			this.builtinEvaluatorFactory = builtinEvaluatorFactory;
		}

		public ComponentFillSubreport createFillSubreport()
		{
			return 
				new ComponentFillSubreport(
					fillContext, subreport, factory, klexReport,
					builtinEvaluatorFactory
					);
		}
	}
}
