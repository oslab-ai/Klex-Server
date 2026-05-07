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

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.KlexPrint;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.ReportContext;
import net.sf.klexreports.interactivity.search.LuceneUtil;
import net.sf.klexreports.search.SpansInfo;
import net.sf.klexreports.web.actions.SearchData;
import net.sf.klexreports.web.servlets.KlexPrintAccessor;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class SearchAction extends AbstractAction {
	
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private SearchData searchData;

	public SearchData getSearchData() {
		return searchData;
	}

	public void setSearchData(SearchData searchData) {
		this.searchData = searchData;
	}

	@Override
	public void performAction() throws ActionException {
		if (searchData != null && searchData.getSearchString() != null && searchData.getSearchString().length() > 0) {
			KlexReportsContext klexReportsContext = getKlexReportsContext();
			ReportContext reportContext = getReportContext();

			KlexPrintAccessor klexPrintAccessor = (KlexPrintAccessor) reportContext.getParameterValue(
					KlexPrintAccessor.REPORT_CONTEXT_PARAMETER_KLEX_PRINT_ACCESSOR);

			KlexPrint klexPrint = klexPrintAccessor.getFinalKlexPrint();
			LuceneUtil luceneUtil = new LuceneUtil(klexReportsContext, searchData.isCaseSensitive(), searchData.isWholeWordsOnly(), searchData.isRemoveAccents());

			try {
				SpansInfo spansInfo = luceneUtil.getSpansInfo(klexPrint, searchData.getSearchString());
				reportContext.setParameterValue("net.sf.klexreports.search.term.highlighter", spansInfo);

				ObjectMapper mapper = new ObjectMapper();
				ObjectNode result = mapper.createObjectNode();

				Map<String, Integer> hitSpansPerPage = spansInfo.getHitSpansPerPage();
				result.put("searchString", searchData.getSearchString());

				ArrayNode searchTerms = mapper.createArrayNode();
				for (String sTerm: spansInfo.getQueryTerms()){
					searchTerms.add(sTerm);
				}
				result.put("searchTerms", searchTerms);

				result.put("termsPerSpan", spansInfo.getTermsPerQuery());

				if (hitSpansPerPage.size() > 0) {
					ArrayNode arrayNode = mapper.createArrayNode();
					ObjectNode item;
					result.set("searchResults", arrayNode);
					for (Map.Entry<String, Integer> entry: hitSpansPerPage.entrySet()) {
						item = mapper.createObjectNode();
						item.put("page", Integer.parseInt(entry.getKey()) + 1);
						item.put("hitCount", entry.getValue());
						arrayNode.add(item);
					}
				}
				reportContext.setParameterValue("net.sf.klexreports.web.actions.result.json", result);

			} catch (Exception e) {
				throw new ActionException(e);
			}
		}
	}

	@Override
	public boolean requiresRefill() {
		return false;
	}

}

