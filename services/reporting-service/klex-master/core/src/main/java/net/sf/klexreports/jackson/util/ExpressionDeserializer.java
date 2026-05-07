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
package net.sf.klexreports.jackson.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;

import net.sf.klexreports.engine.JRExpression;
import net.sf.klexreports.engine.design.JRDesignExpression;
import net.sf.klexreports.engine.type.ExpressionTypeEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class ExpressionDeserializer extends StdDeserializer<JRExpression>
{
	private static final long serialVersionUID = 1L;

	public ExpressionDeserializer()
	{
		this(null);
	}
	
	public ExpressionDeserializer(Class<?> vc)
	{
		super(vc);
	}

	@Override
	public JRExpression deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException 
	{
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		
		JRDesignExpression expression = null;
		
		FromXmlParser xp = p instanceof FromXmlParser ? (FromXmlParser)p : null;
		if (xp == null)
		{
			if (node.isObject())
			{
				expression = new JRDesignExpression(node.get("text").asText());
				JsonNode typeNode = node.get("type");
				if (typeNode != null)
				{
					expression.setType(ExpressionTypeEnum.getByName(typeNode.asText()));
				}
			}
			else
			{
				expression = new JRDesignExpression(node.asText());
			}
		}
		else
		{
			if (node.isObject())
			{
				expression = new JRDesignExpression(node.get("").asText());
				JsonNode typeNode = node.get("type");
				if (typeNode != null)
				{
					expression.setType(ExpressionTypeEnum.getByName(typeNode.asText()));
				}
			}
			else
			{
				expression = new JRDesignExpression(node.asText());
			}
		}
		
		return expression;
    }
}
