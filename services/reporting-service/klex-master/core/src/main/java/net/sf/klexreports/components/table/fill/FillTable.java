/*
 * klexReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
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
package net.sf.klexreports.components.table.fill;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.components.headertoolbar.HeaderToolbarElement;
import net.sf.klexreports.components.subreport.fill.SubreportFillComponent;
import net.sf.klexreports.components.table.BaseColumn;
import net.sf.klexreports.components.table.Column;
import net.sf.klexreports.components.table.ColumnGroup;
import net.sf.klexreports.components.table.ColumnVisitor;
import net.sf.klexreports.components.table.TableComponent;
import net.sf.klexreports.engine.JRDataset;
import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.JRPrintElement;
import net.sf.klexreports.engine.JRPropertiesMap;
import net.sf.klexreports.engine.JRPropertyExpression;
import net.sf.klexreports.engine.JRReport;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.klexPrint;
import net.sf.klexreports.engine.klexReport;
import net.sf.klexreports.engine.design.JRAbstractCompiler;
import net.sf.klexreports.engine.design.JRReportCompileData;
import net.sf.klexreports.engine.export.PdfConstants;
import net.sf.klexreports.engine.fill.BuiltinExpressionEvaluatorFactory;
import net.sf.klexreports.engine.fill.JRFillCloneFactory;
import net.sf.klexreports.engine.fill.JRFillContext;
import net.sf.klexreports.engine.fill.JRFillObjectFactory;
import net.sf.klexreports.engine.util.JRReportUtils;
import net.sf.klexreports.engine.xml.JRXmlWriter;
import net.sf.klexreports.export.AccessibilityUtil;
import net.sf.klexreports.export.type.AccessibilityTagEnum;

/**
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class FillTable extends SubreportFillComponent
{

	private static final Log log = LogFactory.getLog(FillTable.class);
	
	protected static final String FILL_CACHE_KEY_TABLE_INSTANCE_COUNTER = FillTable.class.getName() + "#instanceCounter";
	protected static final String EXCEPTION_MESSAGE_KEY_UNSUPPORTED_REPORT_DATA_TYPE = "components.table.unsupported.report.data.type";
	
	private final TableComponent table;
	private Map<List<FillColumn>, ComponentFillSubreportFactory> fillSubreportFactories;
	
	private boolean filling;
	private List<FillColumn> fillColumns;

	public FillTable(TableComponent table, JRFillObjectFactory factory)
	{
		super(table, factory);
		
		this.table = table;
		
		this.fillSubreportFactories = new HashMap<>();
	}

	public FillTable(FillTable table, JRFillCloneFactory factory)
	{
		super(table, factory);
		
		this.table = table.table;

		this.fillSubreportFactories = table.fillSubreportFactories;
	}
	
	private TableReport getBaseReport()
	{
		return ((TableklexReport)fillSubreport.getklexReport()).getBaseReport();
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
		
		evaluateColumns(evaluation);
		if (!fillColumns.isEmpty())
		{
			createFillSubreport();
			setTableInstanceCounter();
			fillSubreport.evaluateSubreport(evaluation);
		}
	}
	
	protected void setTableInstanceCounter()
	{
		JRFillContext fillerContext = fillContext.getFiller().getFillContext();
		AtomicInteger counter = (AtomicInteger) fillerContext.getFillCache(FILL_CACHE_KEY_TABLE_INSTANCE_COUNTER);
		if (counter == null)
		{
			// we just need a mutable integer, there's no actual concurrency here
			counter = new AtomicInteger();
			fillerContext.setFillCache(FILL_CACHE_KEY_TABLE_INSTANCE_COUNTER, counter);
		}
		
		int instanceIndex = counter.getAndIncrement();
		if (log.isDebugEnabled())
		{
			log.debug("table instance index is " + instanceIndex);
		}
		
		getBaseReport().setTableInstanceIndex(instanceIndex);
	}

	protected boolean toPrintColumn(BaseColumn column, byte evaluation) throws JRException
	{
		boolean toPrint;
		JRExpression printWhenExpression = column.getPrintWhenExpression();
		if (printWhenExpression == null)
		{
			toPrint = true;
		}
		else
		{
			Boolean printWhenVal = (Boolean) evaluateExpression(
					printWhenExpression, evaluation);
			if (printWhenVal == null)
			{
				toPrint = false;
			}
			else
			{
				toPrint = printWhenVal;
			}
		}
		return toPrint;
	}
	
	protected JRPropertiesMap evaluateProperties(BaseColumn column, byte evaluation) throws JRException
	{
		JRPropertiesMap staticProperties = column.hasProperties() ? column.getPropertiesMap().cloneProperties() : null;
		JRPropertiesMap mergedProperties = null;

		JRPropertyExpression[] propExprs = column.getPropertyExpressions();
		if (propExprs == null || propExprs.length == 0)
		{
			mergedProperties = staticProperties;
		}
		else
		{
			JRPropertiesMap dynamicProperties = new JRPropertiesMap();
			
			for (int i = 0; i < propExprs.length; i++)
			{
				JRPropertyExpression prop = propExprs[i];
				String value = (String) evaluateExpression(prop.getValueExpression(), evaluation);
				//if (value != null) //for some properties such as data properties in metadata exporters, the null value is significant
				{
					dynamicProperties.setProperty(prop.getName(), value);
				}
			}
			
			mergedProperties = dynamicProperties.cloneProperties();
			mergedProperties.setBaseProperties(staticProperties);
		}
		
		return mergedProperties;
	}
	
	protected class FillColumnEvaluator implements ColumnVisitor<FillColumn>
	{
		final byte evaluation;
		
		public FillColumnEvaluator(byte evaluation)
		{
			this.evaluation = evaluation;
		}

		@Override
		public FillColumn visitColumn(Column column)
		{
			try
			{
				boolean toPrint = toPrintColumn(column, evaluation);
				if (toPrint)
				{
					JRPropertiesMap properties = evaluateProperties(column, evaluation);
					return new FillColumn(column, properties); 
				}
				return null;
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
		}

		@Override
		public FillColumn visitColumnGroup(ColumnGroup columnGroup)
		{
			try
			{
				boolean toPrint = toPrintColumn(columnGroup, evaluation);
				FillColumn fillColumn;
				if (toPrint)
				{
					List<BaseColumn> columns = columnGroup.getColumns();
					List<FillColumn> subColumns = new ArrayList<>(columns.size());
					int printWidth = 0;
					for (BaseColumn column : columns)
					{
						FillColumn fillSubColumn = column.visitColumn(this);
						if (fillSubColumn != null)
						{
							printWidth += fillSubColumn.getWidth();
							subColumns.add(fillSubColumn);
						}
					}
					
					if (subColumns.isEmpty())
					{
						// no sub columns prints
						// the column group won't print either
						fillColumn = null;
					}
					else
					{
						JRPropertiesMap properties = evaluateProperties(columnGroup, evaluation);
						fillColumn = new FillColumn(columnGroup, printWidth, subColumns, properties);
					}
				}
				else
				{
					fillColumn = null;
				}
				return fillColumn;
			}
			catch (JRException e)
			{
				throw new JRRuntimeException(e);
			}
		}
	}
	
	protected void evaluateColumns(byte evaluation)
	{
		FillColumnEvaluator columnEvaluator = new FillColumnEvaluator(evaluation);
		List<BaseColumn> columns = table.getColumns();
		fillColumns = new ArrayList<>(columns.size());
		fillWidth = 0;
		for (BaseColumn column : columns)
		{
			FillColumn fillColumn = column.visitColumn(columnEvaluator);
			if (fillColumn != null)
			{
				fillColumns.add(fillColumn);
				fillWidth += fillColumn.getWidth();
			}
		}
	}
	
	@Override
	public ComponentFillSubreportFactory getFillSubreportFactory()
	{
		return fillSubreportFactories.get(fillColumns);
	}
	
	@Override
	public void setFillSubreportFactory(ComponentFillSubreportFactory subreportFactory)
	{
		fillSubreportFactories.put(fillColumns, subreportFactory);
	}
	
	@Override
	public boolean isEmpty()
	{
		return fillColumns.isEmpty();
	}

	@Override
	public klexReport getklexReport(BuiltinExpressionEvaluatorFactory builtinEvaluatorFactory) throws JRException
	{
		klexReport parentReport = fillContext.getFiller().getklexReport();
		klexReport containingReport = containingReport(parentReport);
		JRDataset reportSubdataset = JRReportUtils.findSubdataset(table.getDatasetRun(), (JRReport)containingReport);
		
//		BuiltinExpressionEvaluatorFactory builtinEvaluatorFactory = new BuiltinExpressionEvaluatorFactory();
		
		String tableReportName = JRAbstractCompiler.getUnitName(containingReport, reportSubdataset);
		
		// clone the table subdataset in order to have a different instance for other
		// elements that might be using it.
		// we're cloning the subdataset via an object factory in order to preserve
		// the relationship between objects (e.g. variables and groups) in the cloned
		// dataset
		JRDataset tableSubdataset = DatasetCloneObjectFactory.cloneDataset(reportSubdataset);
		TableReportDataset reportDataset = new TableReportDataset(tableSubdataset, tableReportName);

		TableReport tableReport = new TableReport(fillContext, table, reportDataset, fillColumns, builtinEvaluatorFactory);
		
		if (log.isDebugEnabled())
		{
			String tableReportXml = new JRXmlWriter(fillContext.getFiller().getklexReportsContext()).write(tableReport, "UTF-8");
			log.debug("Generated table report:\n" + tableReportXml);
		}
		
		JRReportCompileData tableReportCompileData = createTableReportCompileData(
				containingReport, reportSubdataset);
		
		return 
			new TableklexReport(parentReport, tableReport, 
				tableReportCompileData, 
				new TableReportBaseObjectFactory(reportDataset),
				"");// no suffix as already included in the report name
	}

	protected klexReport containingReport(klexReport parentReport)
	{
		klexReport containingReport = parentReport;
		while (containingReport instanceof TableklexReport)
		{
			containingReport = ((TableklexReport) containingReport).getParentReport();
		}
		return containingReport;
	}
	
	protected JRReportCompileData createTableReportCompileData(
			klexReport parentReport, JRDataset reportSubdataset)
			throws JRException
	{
		Serializable reportCompileDataObj = parentReport.getCompileData();
		if (!(reportCompileDataObj instanceof JRReportCompileData))
		{
			throw 
				new JRRuntimeException(
					EXCEPTION_MESSAGE_KEY_UNSUPPORTED_REPORT_DATA_TYPE,  
					new Object[]{reportCompileDataObj.getClass().getName()} 
					);
		}
		
		JRReportCompileData reportCompileData = (JRReportCompileData) reportCompileDataObj;
		Serializable datasetCompileData = reportCompileData.getDatasetCompileData(
				reportSubdataset);
		
		TableReportCompileData tableReportCompileData = new TableReportCompileData(
				parentReport);
		tableReportCompileData.setMainDatasetCompileData(datasetCompileData);
		
		JRDataset[] datasets = parentReport.getDatasets();
		if (datasets != null)
		{
			for (JRDataset dataset : datasets)
			{
				Serializable compileData = reportCompileData.getDatasetCompileData(dataset);
				tableReportCompileData.setDatasetCompileData(dataset, compileData);
			}
		}
		tableReportCompileData.copyCrosstabCompileData(reportCompileData);
		return tableReportCompileData;
	}
	
	@Override
	public JRPrintElement fill()
	{
		JRPrintElement printFrame = super.fill();
		
		if (getBaseReport().isInteractiveTable()) {
			printFrame.getPropertiesMap().setProperty(HeaderToolbarElement.PROPERTY_TABLE_UUID, fillContext.getComponentElement().getUUID().toString());
		}

		if (getBaseReport().isAccessibleTable())
		{
			printFrame.getPropertiesMap().setProperty(PdfConstants.PROPERTY_TAG_TABLE, PdfConstants.TAG_FULL);
			printFrame.getPropertiesMap().setProperty(AccessibilityUtil.PROPERTY_ACCESSIBILITY_TAG, AccessibilityTagEnum.TABLE.getName());
		}
		
		fillContext.getFiller().getPropertiesUtil().transferProperties(
			fillContext.getComponentElement(), 
			printFrame, klexPrint.PROPERTIES_PRINT_TRANSFER_PREFIX
			);

		return printFrame;
	}
}
