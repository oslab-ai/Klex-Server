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
package net.sf.klexreports.json.expression.member.evaluation;

import java.util.ArrayList;
import java.util.List;

import net.sf.klexreports.json.JRJsonNode;
import net.sf.klexreports.json.JsonNodeContainer;
import net.sf.klexreports.json.expression.EvaluationContext;
import net.sf.klexreports.json.expression.member.MemberExpression;
import net.sf.klexreports.json.expression.member.MultiLevelUpExpression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class MultiLevelUpExpressionEvaluator extends AbstractMemberExpressionEvaluator {
    private static final Log log = LogFactory.getLog(MultiLevelUpExpressionEvaluator.class);

    private MultiLevelUpExpression expression;


    public MultiLevelUpExpressionEvaluator(EvaluationContext evaluationContext, MultiLevelUpExpression expression) {
        super(evaluationContext);
        this.expression = expression;
    }

    @Override
    public JsonNodeContainer evaluate(JsonNodeContainer contextNode) {
        List<JRJsonNode> nodes = contextNode.getNodes();

        if (log.isDebugEnabled()) {
            log.debug("---> evaluating expression [" + expression +
                    "] on a node with (size: " + contextNode.getSize() +
                    ", cSize: " + contextNode.getContainerSize() + ")");
        }

        JsonNodeContainer result = new JsonNodeContainer();
        List<JRJsonNode> uniqueParents = new ArrayList<>();

        for (JRJsonNode node: nodes) {
            JRJsonNode parent = getParent(node);

            if (parent != null && !uniqueParents.contains(parent)) {
                uniqueParents.add(parent);

                if (applyFilter(parent)) {
                    result.add(parent);
                }
            }
        }

        if (result.getSize() > 0) {
            return result;
        }

        return null;
    }

    @Override
    public MemberExpression getMemberExpression() {
        return expression;
    }

    private JRJsonNode getParent(JRJsonNode jrJsonNode) {
        JRJsonNode result = jrJsonNode;
        int level = expression.getLevel();

        for(int i=0; i < level; i++) {
            result = result.getParent();

            if (result == null) {
                return null;
            }
        }

        return result;
    }

}
