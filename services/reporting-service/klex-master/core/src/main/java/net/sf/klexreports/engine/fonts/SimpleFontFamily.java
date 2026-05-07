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
package net.sf.klexreports.engine.fonts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRCloneable;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.engine.util.JRDataUtils;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class SimpleFontFamily implements FontFamily, JRCloneable {

	/**
	 * 
	 */
	private KlexReportsContext klexReportsContext;
	private String name;
	private SimpleFontFace normalFace;
	private SimpleFontFace boldFace;
	private SimpleFontFace italicFace;
	private SimpleFontFace boldItalicFace;
	private String pdfEncoding;
	private Boolean isPdfEmbedded;
	private String defaultExportFont;
	private Map<String, String> exportFonts;
	private Set<String> locales;
	private boolean isVisible = true;

	/**
	 * @see #SimpleFontFamily(KlexReportsContext)
	 */
	public SimpleFontFamily() {
		this(DefaultKlexReportsContext.getInstance());
	}

	/**
	 * 
	 */
	public SimpleFontFamily(KlexReportsContext klexReportsContext) {
		this.klexReportsContext = klexReportsContext;
	}

	@Override
	public Object clone() {
		try {
			SimpleFontFamily clone = (SimpleFontFamily) super.clone();
			if (normalFace != null)
				clone.setNormalFace((SimpleFontFace) normalFace.clone());
			if (boldFace != null)
				clone.setBoldFace((SimpleFontFace) boldFace.clone());
			if (italicFace != null)
				clone.setItalicFace((SimpleFontFace) italicFace.clone());
			if (boldItalicFace != null)
				clone.setBoldItalicFace((SimpleFontFace) boldItalicFace.clone());
			if (locales != null)
				clone.setLocales(new HashSet<>(locales));
			if (exportFonts != null)
				clone.setExportFonts(new HashMap<>(exportFonts));
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * 
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @deprecated Replaced by {@link #setNormalFace(SimpleFontFace)}.
	 */
	public void setNormal(String normal) {
		if (normalFace == null) {
			normalFace = new SimpleFontFace(klexReportsContext);
		}
		normalFace.setTtf(normal);
	}

	/**
	 * @deprecated Replaced by {@link #setBoldFace(SimpleFontFace)}.
	 */
	public void setBold(String bold) {
		if (boldFace == null) {
			boldFace = new SimpleFontFace(klexReportsContext);
		}
		boldFace.setTtf(bold);
	}

	/**
	 * @deprecated Replaced by {@link #setItalicFace(SimpleFontFace)}.
	 */
	public void setItalic(String italic) {
		if (italicFace == null) {
			italicFace = new SimpleFontFace(klexReportsContext);
		}
		italicFace.setTtf(italic);
	}

	/**
	 * @deprecated Replaced by {@link #setBoldItalicFace(SimpleFontFace)}.
	 */
	public void setBoldItalic(String boldItalic) {
		if (boldItalicFace == null) {
			boldItalicFace = new SimpleFontFace(klexReportsContext);
		}
		boldItalicFace.setTtf(boldItalic);
	}

	@Override
	public FontFace getNormalFace() {
		return normalFace;
	}

	/**
	 * 
	 */
	public void setNormalFace(SimpleFontFace normalFace) {
		this.normalFace = normalFace;
	}

	@Override
	public FontFace getBoldFace() {
		return boldFace;
	}

	/**
	 * 
	 */
	public void setBoldFace(SimpleFontFace boldFace) {
		this.boldFace = boldFace;
	}

	@Override
	public FontFace getItalicFace() {
		return italicFace;
	}

	/**
	 * 
	 */
	public void setItalicFace(SimpleFontFace italicFace) {
		this.italicFace = italicFace;
	}

	@Override
	public FontFace getBoldItalicFace() {
		return boldItalicFace;
	}

	/**
	 * 
	 */
	public void setBoldItalicFace(SimpleFontFace boldItalicFace) {
		this.boldItalicFace = boldItalicFace;
	}

	/**
	 * @deprecated Replaced by {@link SimpleFontFace#setPdf(String)}.
	 */
	public void setNormalPdfFont(String normalPdfFont) {
		if (normalFace == null) {
			normalFace = new SimpleFontFace(klexReportsContext);
		}
		normalFace.setPdf(normalPdfFont);
	}

	/**
	 * @deprecated Replaced by {@link SimpleFontFace#setPdf(String)}.
	 */
	public void setBoldPdfFont(String boldPdfFont) {
		if (boldFace == null) {
			boldFace = new SimpleFontFace(klexReportsContext);
		}
		boldFace.setPdf(boldPdfFont);
	}

	/**
	 * @deprecated Replaced by {@link SimpleFontFace#setPdf(String)}.
	 */
	public void setItalicPdfFont(String italicPdfFont) {
		if (italicFace == null) {
			italicFace = new SimpleFontFace(klexReportsContext);
		}
		italicFace.setPdf(italicPdfFont);
	}

	/**
	 * @deprecated Replaced by {@link SimpleFontFace#setPdf(String)}.
	 */
	public void setBoldItalicPdfFont(String boldItalicPdfFont) {
		if (boldItalicFace == null) {
			boldItalicFace = new SimpleFontFace(klexReportsContext);
		}
		boldItalicFace.setPdf(boldItalicPdfFont);
	}

	@Override
	public String getPdfEncoding() {
		return pdfEncoding;
	}

	/**
	 * 
	 */
	public void setPdfEncoding(String pdfEncoding) {
		this.pdfEncoding = pdfEncoding;
	}

	@Override
	public Boolean isPdfEmbedded() {
		return isPdfEmbedded;
	}

	/**
	 * 
	 */
	public void setPdfEmbedded(Boolean isPdfEmbedded) {
		this.isPdfEmbedded = isPdfEmbedded;
	}

	/**
	 * 
	 */
	public String getDefaultExportFont() {
		return defaultExportFont;
	}

	/**
	 * 
	 */
	public void setDefaultExportFont(String defaultExportFont) {
		this.defaultExportFont = defaultExportFont;
	}

	/**
	 * 
	 */
	public Map<String, String> getExportFonts() {
		return exportFonts;
	}

	/**
	 * 
	 */
	public void setExportFonts(Map<String, String> exportFonts) {
		this.exportFonts = exportFonts;
	}

	@Override
	public String getExportFont(String key) {
		String exportFont = exportFonts == null ? null : (String) exportFonts.get(key);
		return exportFont == null ? defaultExportFont : exportFont;
	}

	/**
	 * 
	 */
	public Set<String> getLocales() {
		return locales;
	}

	/**
	 * 
	 */
	public void setLocales(Set<String> locales) {
		this.locales = locales;
	}

	@Override
	public boolean supportsLocale(Locale locale) {
		return locales == null || locales.isEmpty() || locales.contains(JRDataUtils.getLocaleCode(locale));
	}

	@Override
	public boolean isVisible() {
		return isVisible;
	}

	/**
	 * 
	 */
	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

}
