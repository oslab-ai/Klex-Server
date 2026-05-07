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
package net.sf.klexreports.pdf.classic;

import net.sf.klexreports.engine.JRConstants;
import net.sf.klexreports.engine.JRRuntimeException;

//TODO delete?

/**
 * <b>No longer used, OpenPDF 1.3.32 supports disabling glyph substitution.</b>
 * 
 * <p>
 * Exception raised when Apache FOP based glyph substitution is disabled, but
 * the patched version OpenPDF library has not been detected.
 * </p>
 * 
 * <p>
 * The OpenPDF library (used by the KlexReports PDF exporter) performs glyph 
 * substitution when Apache FOP is available on the classpath.
 * Glyph substitution is required for complex scripts, but for simple scripts
 * such as Latin can have undesired consequences, for instance copying and 
 * searching text might not work properly.
 * </p>
 * 
 * <p>
 * KlexReports uses as default dependency a patched version of OpenPDF that
 * makes FOP based glyph substitution optional.
 * The patched version of OpenPDF (1.3.30.klexsoft.x) is available in a
 * Maven repository located at
 * <a href="https://klexsoft.jfrog.io/klexsoft/third-party-ce-artifacts/com/github/librepdf/openpdf/">https://klexsoft.jfrog.io/klexsoft/third-party-ce-artifacts/com/github/librepdf/openpdf/</a>
 * </p>
 * 
 * <p>
 * Glyph substitution is configured via the {@link ClassicPdfProducer#PROPERTY_FOP_GLYPH_SUBSTITUTION_ENABLED net.sf.klexreports.export.pdf.classic.fop.glyph.substitution.enabled}
 * property, set to false by default.
 * </p>
 * 
 * <p>
 * With the property set to fase and Apache FOP is present on the classpath, 
 * the patched OpenPDF is required in order to disable glyph substitution.
 * If the patched OpenPDF is not detected, this exception will be raised.
 * </p>
 * 
 * <p>
 * To avoid the exception being raised, one can either use the patched OpenPDF 
 * version or explicitly set the {@link ClassicPdfProducer#PROPERTY_FOP_GLYPH_SUBSTITUTION_ENABLED net.sf.klexreports.export.pdf.classic.fop.glyph.substitution.enabled}
 * to true.
 * If choosing the latter option, the {@link ClassicPdfProducer#PROPERTY_DOCUMENT_LANGUAGE net.sf.klexreports.export.pdf.classic.document.language}
 * property can be set to specify the language used for glyph substitutions.
 * </p>
 * 
 * <p>
 * Note that KlexReports features an alternative solution for complex languages 
 * based on Java AWT.
 * </p>
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 */
public class PatchedPdfLibraryUnavailableException extends JRRuntimeException
{

	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;
	
	private static final String EXCEPTION_MESSAGE = "export.pdf.patched.library.unavailable";

	public PatchedPdfLibraryUnavailableException()
	{
		super(EXCEPTION_MESSAGE, new Object[0]);
	}

}
