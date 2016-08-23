
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

import com.henrikhedberg.util.TcpServer;
import com.henrikhedberg.util.TcpServerConnection;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Open Home Automation Protocol (OHAP) server with TCP backend.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20160320)
 */

public class TcpOhapServer extends OhapServer {
	private TcpServer tcpServer;
	
	public TcpOhapServer(SocketAddress socketAddress) throws IOException {
		tcpServer = new TcpServer(socketAddress) {
			public void acceptConnection(TcpServerConnection connection) {
				try {
					new TcpOhapSession(TcpOhapServer.this, connection);
				} catch (IOException e) {
				}
			}
		};
	}
	
	public TcpServer getTcpServer() {
		return tcpServer;
	}

	public static void main(String[] args) throws IOException {
		TcpOhapServer ohapServer = new TcpOhapServer(new InetSocketAddress(18001));
		ohapServer.getTcpServer().run();
	}
	
	private static class TcpOhapSession extends OhapSession {
		TcpServerConnection connection;
		private IncomingMessage incomingMessage = new IncomingMessage();
		private ByteBuffer incomingByteBuffer = ByteBuffer.allocate(1024);

		TcpOhapSession(OhapServer server, TcpServerConnection connection) throws IOException {
			super(server, connection.getRemoteAddress().toString());
			this.connection = connection;
			connection.setHandler(new TcpServerConnection.Handler() {
				public void handleData(TcpServerConnection connection) {
					try {
						connection.read(incomingByteBuffer);
						incomingByteBuffer.flip();
						if (incomingMessage.readFromNB(incomingByteBuffer))
							handleMessage(incomingMessage);
						incomingByteBuffer.compact();
						if (!incomingByteBuffer.hasRemaining()) {
							incomingByteBuffer.flip();
							incomingByteBuffer =
							    ByteBuffer.allocate(incomingByteBuffer.capacity() * 2)
							    .put(incomingByteBuffer);
						}

					} catch (IOException e) {
						readMessageFailed(e);
					}
				  }
				  
				  public void handleIOException(IOException exception) {
				  	// TODO
				  }
			});
		}

		protected void writeMessage(OutgoingMessage outgoingMessage) throws IOException {
			ByteBuffer outgoingByteBuffer = outgoingMessage.asByteBuffer();
			connection.write(outgoingByteBuffer);
		}
		
		protected void close() throws IOException {
			connection.close();
		}
	}
}
