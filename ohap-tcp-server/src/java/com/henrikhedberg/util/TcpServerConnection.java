
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

import java.nio.*;
import java.nio.channels.*;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.io.IOException;

/**
 * An established TCP connection in {@link TcpServer}.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20160312)
 */
public class TcpServerConnection {
	private TcpServer tcpServer;
	private SocketChannel socketChannel;
	private Handler handler;
	private LinkedList<ByteBuffer> writeBuffers = new LinkedList<>();
	private SelectorLoop.WritableHandler writableHandler;
	
	TcpServerConnection(TcpServer tcpServer, SocketChannel socketChannel) throws IOException {
		this.tcpServer = tcpServer;
		this.socketChannel = socketChannel;

		socketChannel.configureBlocking(false);
		tcpServer.registerReadableHandler(socketChannel, new SelectorLoop.ReadableHandler() {
			public void handleReadable(SelectableChannel channel) {
				if (handler != null)
					handler.handleData(TcpServerConnection.this);
			}
		});
	}
	
	public void close() throws IOException {
		tcpServer.registerReadableHandler(socketChannel, null);
		socketChannel.close();
	}
	
	public SocketAddress getRemoteAddress() throws IOException {
		return socketChannel.getRemoteAddress();
	}
	
	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	public int read(ByteBuffer buffer) throws IOException {
		return socketChannel.read(buffer);
	}
	
	public boolean write(ByteBuffer buffer) throws IOException {
		if (writeBuffers.size() == 0) {
			socketChannel.write(buffer);
			if (!buffer.hasRemaining())
				return true;

			if (writableHandler == null)
				writableHandler = new WritableHandler();
			tcpServer.registerWritableHandler(socketChannel, writableHandler);
		}
		
		writeBuffers.add(buffer);

		return false;
	}

	public static interface Handler {
		public void handleData(TcpServerConnection connection);
		public void handleIOException(IOException exception);
	}
	
	private class WritableHandler implements SelectorLoop.WritableHandler {
		public void handleWritable(SelectableChannel channel) {
			try {
				ByteBuffer buffer = writeBuffers.element();
				socketChannel.write(buffer);
				if (!buffer.hasRemaining()) {
					writeBuffers.remove();
					if (writeBuffers.size() == 0)
						tcpServer.registerWritableHandler(socketChannel, null);
				}
			} catch (IOException e) {
				if (handler != null)
					handler.handleIOException(e);
			}
		}
	}
}
