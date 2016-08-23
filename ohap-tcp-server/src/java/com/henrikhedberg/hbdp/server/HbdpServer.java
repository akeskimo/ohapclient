
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
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.net.InetAddress;

/**
 * HTTP Bidirectional Protocol (HBDP) server.
 *
 * <p>The implementation relies on the <code>com.sun.new.httpserver</code>
 * package.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20150503)
 */
public class HbdpServer {
	private HttpContext httpContext;
	private String contextPath;
	private HbdpConnection.Handler connectionHandler;
	private HashMap<String, HbdpConnection> connections = new HashMap<>();
	private Random random = new Random();
	
	/**
	 * Instantiates a new {@link HbdpServer} within the given {@link HttpContext}.
	 * The {@link HbdpConnection.Handler} is called for every new connection.
	 *
	 * @param httpContext the HTTP context to set this server as a handler
	 * @param connectionHandler a handler that is called for every new connection
	 */
	public HbdpServer(HttpContext httpContext, HbdpConnection.Handler connectionHandler) {
		this.httpContext = httpContext;
		contextPath = httpContext.getPath();;
		this.connectionHandler = connectionHandler;
		
		httpContext.setHandler(new HttpHandler() {
			public void handle(HttpExchange exchange) throws IOException {
				try {
					handleHbdp(exchange);
				} catch (HbdpException exception) {
					log(exchange.getRemoteAddress().getAddress(), null, "HbdpException: " + exception.getMessage());
					sendResponse(exchange, exception.getCode(), exception.getMessage());
				} catch (IOException exception) {
					log(exchange.getRemoteAddress().getAddress(), null, "IOException: " + exception.getMessage());
					sendResponse(exchange, 500, exception.toString());
/*				} catch (Exception exception) {
					log(exchange.getRemoteAddress().getAddress(), null, "Exception: " + exception.getMessage());
					exception.printStackTrace();
					sendResponse(exchange, 500, exception.toString());
*/				}
			}
		});
	}
	
	private void handleHbdp(HttpExchange exchange) throws HbdpException, IOException {
		String path = exchange.getRequestURI().getPath();
		if (!path.startsWith(contextPath))
			throw new HbdpException(404, "Wrong context path.");
		path = path.substring(contextPath.length());
		
		String method = exchange.getRequestMethod();
		if (path.length() == 0) {
			if (!method.equals("GET"))
				throw new HbdpException(405, "Only GET method allowed for session initialisation.");

			String identifier;
			do {
				identifier = generateUid();
			} while (connections.get(identifier) != null);

			log(exchange.getRemoteAddress().getAddress(), identifier, "Connected");

			HbdpConnection connection = new HbdpConnection(identifier);
			connections.put(identifier, connection);
			connectionHandler.handle(connection);
			sendResponse(exchange, 200, identifier);
		} else {
			int index = path.indexOf('/');
			if (index == -1 && index + 1 == path.length())
				throw new HbdpException(404, "No serial number.");
			if (index == 0)
				throw new HbdpException(404, "No identifier.");
			if (path.indexOf('/', index + 1) != -1)
				throw new HbdpException(404, "Too many slashes (/).");
			String identifier = path.substring(0, index);
			long serial;
			try {
				serial = Long.parseLong(path.substring(index + 1));
			} catch (NumberFormatException exception) {
				throw new HbdpException(404, "Serial is not a number.");
			}

			HbdpConnection connection = connections.get(identifier);
			if (connection == null)
				throw new HbdpException(404, "No session with the provided identifier.");
			
			if (method.equals("DELETE")) {
				connections.remove(identifier);
				log(exchange.getRemoteAddress().getAddress(), identifier, "Client disconnected");
			} else if (method.equals("POST")) {
				if (!connection.handle(exchange, serial)) {
					connections.remove(identifier);
					log(exchange.getRemoteAddress().getAddress(), identifier, "Server disconnected");				
				}
			} else
				throw new HbdpException(405, "Only POST or DELETE method allowed for session requests.");			}
	}
	
	private String generateUid() {
		byte[] bytes = new byte[16];
		random.nextBytes(bytes);
		StringBuffer buffer = new StringBuffer();
		for (byte b : bytes) {
			int i = b - Byte.MIN_VALUE;
			if (i < 16)
				buffer.append('0');
			buffer.append(Integer.toHexString(i));
		}
		
		return buffer.toString();
	}
	
	private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
		sendResponse(exchange, code, "text/plain; charset=utf-8", body.getBytes(Charset.forName("UTF-8")));
	}

	private void sendResponse(HttpExchange exchange, int code, byte[] body) throws IOException {
		sendResponse(exchange, code, "application/octet-stream", body);
	}
	
	private void sendResponse(HttpExchange exchange, int code, String contentType, byte[] body) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", contentType);
		exchange.sendResponseHeaders(code, body.length);
		OutputStream output = exchange.getResponseBody();
		output.write(body);
		output.close();
	}
	
	private void log(InetAddress address, String identifier, String detail) {
		long seconds = System.currentTimeMillis() / 1000;
		System.out.println(seconds + "  " + (identifier != null ? identifier : "\t\t\t\t") + "  Hbdp  " + detail + "  (" + address.getHostAddress() + ")");
	}
}
