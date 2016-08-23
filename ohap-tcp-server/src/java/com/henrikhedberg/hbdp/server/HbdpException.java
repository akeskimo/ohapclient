
/*
 * HTTP Bidirectional Protocol (HBDP) Reference Implementation
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

package com.henrikhedberg.hbdp.server;

/**
 * An exception containing also an HTTP status code.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20150503)
 */
class HbdpException extends Exception {
	private int code;

	/**
	 * Constructs a new exception with the given HTTP status
	 * code message.
	 *
	 * @param code HTTP status code
	 * @param message textual message
	 */
	public HbdpException(int code, String message) {
		super(message);
		this.code = code;
	}
	
	/**
	 * Returns the HTTP status code assosiated with the exception.
	 *
	 * @return HTTP status code
	 */
	public int getCode() {
		return code;
	}
}
