
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
import com.sun.net.httpserver.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An example HBDP server that simply echoes all received bytes back to the
 * client. See the source code for more information.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20150503)
 */
public class EchoHbdpServer implements HbdpConnection.Handler {
	private Timer timer = new Timer();

	public EchoHbdpServer(int port) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 10);
		HttpContext context = server.createContext("/");
		new HbdpServer(context, this);
		server.setExecutor(null);
		server.start();	
	}

	public void handle(HbdpConnection connection) {
		final OutputStream outputStream = connection.getOutputStream();

/*		timer.scheduleAtFixedRate(new TimerTask() {
			private int counter = 5;
		
			public void run() {
				try {
					outputStream.write('\n');
					counter--;
					if (counter == 0) {
						outputStream.close();
						cancel();
					}
				} catch (IOException exception) {
				}
			}
		}, 2000, 2000);
*/		
		connection.setInputStreamHandler(new InputStreamHandler() {
			public void handle(InputStream inputStream) throws IOException {
				while (inputStream.available() > 0) {
					int data = inputStream.read();
					if (data == -1) {
						inputStream.close();
						outputStream.close();
					} else
						outputStream.write(data);
				}
			}
		});
	}

	public static void main(String[] args) throws IOException {
		new EchoHbdpServer(8080);
	}
}
