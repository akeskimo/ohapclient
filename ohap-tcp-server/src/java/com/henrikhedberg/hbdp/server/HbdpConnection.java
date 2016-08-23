
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

import com.henrikhedberg.util.InputStreamHandler;
import com.henrikhedberg.util.BufferInputStream;
import com.henrikhedberg.util.BufferOutputStream;
import com.sun.net.httpserver.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * HTTP Bidirectional Protocol (HBDP) server-side connection.
 *
 * <p>The {@link HbdpServer} instantiates one connection for each
 * incoming session. When a {@link Handler} receive a new connection,
 * it should take the {@link InputStream} with the {@link #getInputStream()}
 * method and the {@link OutputStream} with the {@link #getOutputStream()}
 * method. Optionally, it can also register a {@link InputStreamHandler}, which
 * is called when the {@link InputStream} has new bytes available.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20150503)
 */
public class HbdpConnection {
	private String identifier;
	private InputStreamHandler inputStreamHandler;
	private long currentSerial;
	private HttpExchange currentExchange;
	private boolean handling;
	private BufferInputStream connectionInputStream;
	private BufferOutputStream connectionOutputStream;
	private boolean closing;
	
	HbdpConnection(String identifier) {
		this.identifier = identifier;
		connectionInputStream = new BufferInputStream(1024);
		connectionOutputStream = new BufferOutputStream(1024);
		connectionOutputStream.setHandler(new BufferOutputStream.Handler() {
			public void handleClose(BufferOutputStream outputStream) throws IOException {
				closing = true;
			}
			public void handleWrite(BufferOutputStream outputStream) throws IOException {
				if (!handling && currentExchange != null)
					sendOutput();
			}
		});
	}
	
	/**
	 * Sets optional {@link InputStreamHandler}, which is called when the
	 * underlying {@link InputStream} has new bytes available.
	 *
	 * @param handler a handler that is called when new bytes are available
	 * @see #getInputStream()
	 */
	public void setInputStreamHandler(InputStreamHandler handler) {
		inputStreamHandler = handler;
	}
	
	/**
	 * Returns the underlying {@link InputStream} that is used to read
	 * bytes received from the HBDP session.
	 *
	 * @return input stream over the HTTP from the HBDP client
	 */
	public InputStream getInputStream() {
		return connectionInputStream;
	}
	
	/**
	 * Returns the underlying {@link OutputStream} that is used to write
	 * bytes to be transmit through the HBDP session.
	 *
	 * @return output stream over the HTTP to the HBDP client
	 */
	public OutputStream getOutputStream() {
		return connectionOutputStream;
	}

	/**
	 * Returns the session identifier for this connection.
	 *
	 * @return session identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}

	boolean handle(HttpExchange exchange, long serial) throws HbdpException, IOException {
		if (currentSerial != serial)
			throw new HbdpException(404, "Wrong serial number: expected " + currentSerial + ", got " + serial + ".");
		currentSerial++;

		if (currentExchange != null) {
			currentExchange.sendResponseHeaders(200, 0);
			currentExchange.getResponseBody().close();
		}
		currentExchange = exchange;

		InputStream httpInputStream = exchange.getRequestBody();
		log(currentExchange.getRemoteAddress().getAddress(), "Read " + connectionInputStream.readFrom(httpInputStream) + " bytes");
		httpInputStream.close();
		
		if (inputStreamHandler != null && connectionInputStream.available() > 0) {
			handling = true;
			int available = connectionInputStream.available();
			do {
				inputStreamHandler.handle(connectionInputStream);
				int stillAvailable = connectionInputStream.available();
				if (stillAvailable == available)
					break;
				available = stillAvailable;
			} while (available > 0);
			handling = false;
		}
		
		if (connectionOutputStream.available() > 0)
			sendOutput();

		return !closing;
	}
	
	private void sendOutput() throws IOException {
		OutputStream httpOutputStream = currentExchange.getResponseBody();
		currentExchange.sendResponseHeaders(200, connectionOutputStream.available());
		log(currentExchange.getRemoteAddress().getAddress(), "Wrote " + connectionOutputStream.writeTo(httpOutputStream) + " bytes");
		httpOutputStream.close();
		currentExchange = null;
	}

	private void log(InetAddress address, String detail) {
		long seconds = System.currentTimeMillis() / 1000;
		System.out.println(seconds + "  " + identifier + "  Hbdp  " + detail + "  (" + address.getHostAddress() + ")");
	}
	
	/**
	 * An interface to notify a handler that a new HBDP connection (session)
	 * is established.
	 *
	 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
	 * @version 1.0 (20150503)
	 */
	public static interface Handler {
		/**
		 * Called when a new HBDP connection (session) is established.
		 *
		 * @param connection the new connection
		 */
		public void handle(HbdpConnection connection);
	}
}
