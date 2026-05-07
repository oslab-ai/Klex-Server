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
package net.sf.klexreports.json.expression.filter.evaluation;

import net.sf.klexreports.json.JRJsonNode;
import net.sf.klexreports.json.expression.EvaluationContext;
import net.sf.klexreports.json.expression.filter.BasicFilterExpression;
import net.sf.klexreports.json.expression.filter.CompoundFilterExpression;
import net.sf.klexreports.json.expression.filter.NotFilterExpression;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class DefaultFilterExpressionEvaluatorVisitor implements FilterExpressionEvaluatorVisitor {
    private EvaluationContext evaluationContext;


    public DefaultFilterExpressionEvaluatorVisitor(EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
    }

    @Override
    public boolean evaluateBasicFilter(BasicFilterExpression expression, JRJsonNode contextNode) {
        FilterExpressionEvaluator evaluator = new BasicFilterExpressionEvaluator(evaluationContext, expression);
        return evaluator.evaluate(contextNode);
    }

    @Override
    public boolean evaluateCompoundFilter(CompoundFilterExpression expression, JRJsonNode contextNode) {
        FilterExpressionEvaluator evaluator = new CompoundFilterExpressionEvaluator(evaluationContext, expression);
        return evaluator.evaluate(contextNode);
    }

    @Override
    public boolean evaluateNotFilter(NotFilterExpression expression, JRJsonNode contextNode) {
        FilterExpressionEvaluator evaluator = new NotFilterExpressionEvaluator(evaluationContext, expression);
        return evaluator.evaluate(contextNode);
    }
}
