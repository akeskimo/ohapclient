
/*
 * Open Home Automation Protocol (OHAP) Reference Server Implementation
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

package com.henrikhedberg.ohap;

import com.henrikhedberg.hbdp.server.*;
import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import com.henrikhedberg.util.InputStreamHandler;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
/**
 * Open Home Automation Protocol (OHAP) server with HBDP backend.
 *
 * <p>The implementation relies on the {@link HbdpServer} and
 * the <code>com.sun.new.httpserver</code> package.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20160312)
 */

public class HbdpOhapServer extends OhapServer {
	private HbdpServer hbdpServer;
	
	public HbdpOhapServer(HttpContext httpContext) {
		hbdpServer = new HbdpServer(httpContext, new HbdpConnection.Handler() {
			public void handle(HbdpConnection connection) {
				new HbdpOhapSession(HbdpOhapServer.this, connection);
			}
		});
	}
	
	public HbdpServer getHbdpServer() {
		return hbdpServer;
	}

	public static void main(String[] args) throws IOException {
		HttpServer httpServer = HttpServer.create(new InetSocketAddress(18000), 10);
		HttpContext httpContext = httpServer.createContext("/");
		HbdpOhapServer ohapServer = new HbdpOhapServer(httpContext);
		httpServer.setExecutor(null);
		httpServer.start();
	}
	
	private static class HbdpOhapSession extends OhapSession {
		private InputStream inputStream;
		private OutputStream outputStream;
		private IncomingMessage incomingMessage = new IncomingMessage();;

		HbdpOhapSession(OhapServer server, HbdpConnection connection) {
			super(server, connection.getIdentifier());
			inputStream = connection.getInputStream();
			outputStream = connection.getOutputStream();
			connection.setInputStreamHandler(new InputStreamHandler() {
				public void handle(InputStream inputStream) {
					try {
						if (incomingMessage.readFromNB(inputStream))
							handleMessage(incomingMessage);
					} catch (IOException e) {
						readMessageFailed(e);
					}
				  }
			});
		}

		protected void writeMessage(OutgoingMessage outgoingMessage) throws IOException {
			outgoingMessage.writeTo(outputStream);
		}
		
		protected void close() throws IOException {
			outputStream.close();
		}
	}
}
