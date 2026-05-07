<%--
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
--%>

<%@ page errorPage="error.jsp" %>
<%@ page import="net.sf.klexreports.engine.util.*" %>
<%@ page import="java.io.*" %>

<html>
<body>
<pre>
<%
	String filename = request.getParameter("filename");
	
	if (filename != null)
	{
		InputStream is = new FileInputStream(application.getRealPath(filename));
		InputStreamReader reader = new InputStreamReader(is);
		
		int ln = 0;
		char[] chars = new char[1000];
		
		while((ln = reader.read(chars)) > 0)
		{
			out.print(JRStringUtil.xmlEncode(new String(chars, 0, ln)));
		}
	}
%>
</pre>
</body>
</html>
