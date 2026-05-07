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
package net.sf.klexreports.json.expression;

import java.util.List;

import net.sf.klexreports.json.JRJsonNode;
import net.sf.klexreports.json.JsonNodeContainer;
import net.sf.klexreports.json.expression.filter.evaluation.DefaultFilterExpressionEvaluatorVisitor;
import net.sf.klexreports.json.expression.filter.evaluation.FilterExpressionEvaluatorVisitor;
import net.sf.klexreports.json.expression.member.MemberExpression;
import net.sf.klexreports.json.expression.member.evaluation.DefaultMemberExpressionEvaluatorVisitor;
import net.sf.klexreports.json.expression.member.evaluation.DefaultMemberExpressionEvaluatorVisitorForFilter;
import net.sf.klexreports.json.expression.member.evaluation.MemberExpressionEvaluatorVisitor;
import net.sf.klexreports.json.util.JsonUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JsonQLExpressionEvaluator {

    private EvaluationContext evaluationContext;


    public JsonQLExpressionEvaluator() {
        evaluationContext = new EvaluationContext() {
            @Override
            public FilterExpressionEvaluatorVisitor getFilterExpressionEvaluatorVisitor() {
                return new DefaultFilterExpressionEvaluatorVisitor(this);
            }

            @Override
            public MemberExpressionEvaluatorVisitor getMemberExpressionEvaluatorVisitor() {
                return new DefaultMemberExpressionEvaluatorVisitor(this);
            }

            @Override
            public MemberExpressionEvaluatorVisitor getMemberExpressionEvaluatorVisitorForFilter() {
                return new DefaultMemberExpressionEvaluatorVisitorForFilter(this);
            }

            @Override
            public ObjectMapper getObjectMapper() {
                return JsonUtil.createObjectMapper();
            }
        };
    }

    public JsonNodeContainer evaluate(JsonQLExpression expression, JRJsonNode contextNode) {

        List<MemberExpression> memberExpressionList = expression.getMemberExpressionList();
        JsonNodeContainer result = new JsonNodeContainer(contextNode);

        if (memberExpressionList != null) {
            for (MemberExpression me: memberExpressionList) {
                result = me.evaluate(result, evaluationContext.getMemberExpressionEvaluatorVisitor());

                if (result == null) {
                    return null;
                }
            }
        }

        return result;

    }

    public EvaluationContext getEvaluationContext() {
        return evaluationContext;
    }
}
