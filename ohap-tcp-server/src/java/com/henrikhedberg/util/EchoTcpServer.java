
/*
 * Miscellaneous Java Utilities by Henrik Hedberg
 * Copyright (C) 2016 Henrik Hedberg <henrik.hedberg@iki.fi>
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

import java.nio.ByteBuffer;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.IOException;

/**
 * 
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20160312)
 */
public class EchoTcpServer extends TcpServer {
	private ByteBuffer buffer;

	public EchoTcpServer(SocketAddress address) throws IOException {
		super(address);
		
		buffer = ByteBuffer.allocate(1024);
	}

	protected void acceptConnection(final TcpServerConnection connection) {
		connection.setHandler(new TcpServerConnection.Handler() {
			public void handleData(TcpServerConnection connection) {
				try {
					buffer.clear();
					if (connection.read(buffer) == -1) {
						connection.close();
						return;
					}
					buffer.flip();
					if (connection.write(buffer))
						buffer.clear();
					else
						buffer = ByteBuffer.allocate(1024);
				} catch (IOException exception) {
					try {
						connection.close();
					} catch (IOException e) {
					}
				}
			}
			
			public void handleIOException(IOException exception) {
				try {
					connection.close();
				} catch (IOException e) {
				}
			}
		});
	}
	
	public static void main(String[] args) throws IOException {
		new EchoTcpServer(new InetSocketAddress(1234)).run();
	}
}
