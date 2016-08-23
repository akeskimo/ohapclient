
/*
 * Miscellaneous Java Utilities by Henrik Hedberg
 * Copyright (C) 2015-2016 Henrik Hedberg <henrik.hedberg@iki.fi>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.henrikhedberg.util;

import java.io.InputStream;
import java.io.IOException;

/**
 * An interface to notify a handler that the specified {@link InputStream}
 * has new bytes to be read.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20150503)
 */
public interface InputStreamHandler {

	/**
	 * Called when the specified {@link InputStream}
	 * has new bytes to be read.
	 *
	 * @param inputStream the {@link InputStream} that is ready to be read
	 */
	public void handle(InputStream inputStream) throws IOException;
}
