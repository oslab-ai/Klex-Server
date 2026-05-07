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
package net.sf.klexreports.json.util;

import java.io.StringReader;
import java.util.List;

import net.sf.klexreports.engine.JRException;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.json.JRJsonNode;
import net.sf.klexreports.json.JsonNodeContainer;
import net.sf.klexreports.json.expression.JsonQLExpression;
import net.sf.klexreports.json.expression.JsonQLExpressionEvaluator;
import net.sf.klexreports.json.parser.JsonQueryLexer;
import net.sf.klexreports.json.parser.JsonQueryParser;
import net.sf.klexreports.json.parser.JsonQueryWalker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class DefaultJsonQLExecuter implements JsonQLExecuter {
    private static final Log log = LogFactory.getLog(DefaultJsonQLExecuter.class);

    private JsonQLExpressionEvaluator evaluator;

    public DefaultJsonQLExecuter() {
        evaluator = new JsonQLExpressionEvaluator();
    }

    @Override
    public List<JRJsonNode> selectNodes(JRJsonNode rootNode, String expression) throws JRException {
        JsonNodeContainer container;

        if (expression != null && expression.trim().length() > 0) {
            container = evaluator.evaluate(getJsonQLExpression(expression), rootNode);

            if (container != null) {
                return container.getContainerNodes();
            }
        } else {
            container = new JsonNodeContainer(rootNode);

            return container.getContainerNodes();
        }

        return null;
    }

    @Override
    public JRJsonNode selectNode(JRJsonNode contextNode, JRJsonNode rootNode, String expression) throws JRException {
        if (expression != null  && expression.trim().length() > 0) {
            JsonQLExpression jsonQLExpression = getJsonQLExpression(expression);
            JRJsonNode node = contextNode;

            if (jsonQLExpression.isAbsolute()) {
                node = rootNode;
            }

            JsonNodeContainer container = evaluator.evaluate(jsonQLExpression, node);

            if (container != null) {
                return container.getNodes().get(0);
            }
        } else {
            return contextNode;
        }

        return null;
    }

    public JsonNodeContainer evaluateExpression(JRJsonNode jrJsonNode, String expression) {
        if (expression != null && expression.trim().length() > 0) {
            return evaluator.evaluate(getJsonQLExpression(expression), jrJsonNode);
        }

        return null;
    }

    public JsonQLExpressionEvaluator getEvaluator() {
        return evaluator;
    }

    protected JsonQLExpression getJsonQLExpression(String expression) {
        try {
            JsonQueryLexer lexer = new JsonQueryLexer(new StringReader(expression.trim()));

            JsonQueryParser parser = new JsonQueryParser(lexer);
            parser.pathExpr();

            JsonQueryWalker walker = new JsonQueryWalker();
            return walker.jsonQLExpression(parser.getAST());

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception is of type: " + e.getClass());
            }
            throw new JRRuntimeException(e);
        }
    }

}
