/*
 * KlexReports - Free Java Reporting Library.
 * Copyright (C) 2005 Works, Inc. All rights reserved.
 * http://www.works.com
 * Copyright (C) 2005 - 2023 Cloud Software Group, Inc. All rights reserved.
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

/*
 * Licensed to Klexsoft Corporation under a Contributer Agreement
 */
package net.sf.klexreports.engine.fill;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.klexreports.annotations.properties.Property;
import net.sf.klexreports.annotations.properties.PropertyScope;
import net.sf.klexreports.engine.DefaultKlexReportsContext;
import net.sf.klexreports.engine.JRPropertiesUtil;
import net.sf.klexreports.engine.JRRuntimeException;
import net.sf.klexreports.engine.JRVirtualizable;
import net.sf.klexreports.engine.KlexReportsContext;
import net.sf.klexreports.properties.PropertyConstants;

/**
 * Virtualizes data to the filesystem. When this object is finalized, it removes
 * the swap files it makes. The virtualized objects have references to this
 * object, so finalization does not occur until this object and the objects
 * using it are only weakly referenced.
 * 
 * @author John Bindel
 */
public class JRFileVirtualizer extends JRAbstractLRUVirtualizer {
	
	private static final Log log = LogFactory.getLog(JRFileVirtualizer.class);

	
	/**
	 * Property used to decide whether {@link File#deleteOnExit() deleteOnExit} should be requested
	 * for temporary files created by the virtualizer.
	 * <p>
	 * Calling  {@link File#deleteOnExit() File.deleteOnExit()} will accumulate JVM process memory
	 * (see this <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4513817">bug</a>), and this
	 * should abviously be avoided in long-running applications.
	 * <p>
	 * Temporary files will be deleted by explicitly calling {@link #cleanup() cleanup()} or from the virtualizer
	 * <code>finalize()</code> method.
	 */
	@Property(
			category = PropertyConstants.CATEGORY_FILL,
			defaultValue = PropertyConstants.BOOLEAN_TRUE,
			scopes = {PropertyScope.CONTEXT},
			sinceVersion = PropertyConstants.VERSION_1_2_3,
			valueType = Boolean.class
			)
	public static final String PROPERTY_TEMP_FILES_SET_DELETE_ON_EXIT = JRPropertiesUtil.PROPERTY_PREFIX + "virtualizer.files.delete.on.exit";

	private final KlexReportsContext klexReportsContext;
	private final String directory;

	/**
	 * Uses the process's working directory as the location to store files.
	 * 
	 * @param maxSize
	 *            the maximum size (in JRVirtualizable objects) of the paged in
	 *            cache.
	 */
	public JRFileVirtualizer(int maxSize) {
		this(DefaultKlexReportsContext.getInstance(), maxSize, null);
	}

	/**
	 * @param maxSize
	 *            the maximum size (in JRVirtualizable objects) of the paged in
	 *            cache.
	 * @param directory
	 *            the base directory in the filesystem where the paged out data
	 *            is to be stored
	 */
	public JRFileVirtualizer(int maxSize, String directory) {
		this(DefaultKlexReportsContext.getInstance(), maxSize, directory);
	}

	/**
	 * @param klexReportsContext
	 *            the KlexReportsContext to use for reading configuration from.
	 * @param maxSize
	 *            the maximum size (in JRVirtualizable objects) of the paged in
	 *            cache.
	 * @param directory
	 *            the base directory in the filesystem where the paged out data
	 *            is to be stored
	 */
	public JRFileVirtualizer(KlexReportsContext klexReportsContext, int maxSize, String directory) {
		super(maxSize);
		
		this.klexReportsContext = klexReportsContext;
		this.directory = directory;
	}

	private String makeFilename(JRVirtualizable o) {
		String uid = o.getUID();
		return "virt" + uid;
	}

	private String makeFilename(String virtualId) {
		return "virt" + virtualId;
	}

	@Override
	protected void pageOut(JRVirtualizable o) throws IOException {
		// Store data to a file.
		String filename = makeFilename(o);
		File file = new File(directory, filename);
		
		if (file.createNewFile()) {
			boolean deleteOnExit = JRPropertiesUtil.getInstance(klexReportsContext).getBooleanProperty(PROPERTY_TEMP_FILES_SET_DELETE_ON_EXIT);
			if (deleteOnExit) {
				file.deleteOnExit();
			}

			try (BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(file))) {
				writeData(o, bufferedOut);
			}
			catch (FileNotFoundException e) {
				log.error("Error virtualizing object", e);
				throw new JRRuntimeException(e);
			}
		} else {
			if (!isReadOnly(o)) {
				throw new IllegalStateException(
						"Cannot virtualize data because the file \"" + filename
								+ "\" already exists.");
			}
		}
	}

	@Override
	protected void pageIn(JRVirtualizable o) throws IOException {
		// Load data from a file.
		String filename = makeFilename(o);
		File file = new File(directory, filename);

		try (BufferedInputStream bufferedIn = new BufferedInputStream(new FileInputStream(file))) {
			readData(o, bufferedIn);
		}
		catch (FileNotFoundException e) {
			log.error("Error devirtualizing object", e);
			throw new JRRuntimeException(e);
		}

		if (!isReadOnly(o)) {
			// Wait until we know it worked before tossing the data.
			file.delete();
		}
	}

	@Override
	protected void dispose(String virtualId) {
		String filename = makeFilename(virtualId);
		File file = new File(directory, filename);
		file.delete();
	}
	
	
	/**
	 * Called when we are done with the virtualizer and wish to
	 * cleanup any resources it has.
	 */
	@Override
	public synchronized void cleanup()
	{
		disposeAll();
		reset();
	}
}
