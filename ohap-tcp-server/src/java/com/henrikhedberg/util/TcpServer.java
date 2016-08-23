
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

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectableChannel;
import java.io.IOException;

/**
 * A simple base class for single-threaded TCP servers. Subclasses must implement the
 * {@link #acceptConnection(TcpServerConnection)} method.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20160312)
 */
public abstract class TcpServer extends SelectorLoop {
	private ServerSocketChannel serverSocketChannel;
	
	public TcpServer(SocketAddress address) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(address).configureBlocking(false);
		registerAcceptableHandler(serverSocketChannel, new SelectorLoop.AcceptableHandler() {
			public void handleAcceptable(SelectableChannel channel) {
				handleAccept();
			}
		});
	}

	protected abstract void acceptConnection(TcpServerConnection connection);

	private void handleAccept() {
		try {
			SocketChannel socketChannel = serverSocketChannel.accept();
			if (socketChannel != null) {
				TcpServerConnection connection = new TcpServerConnection(this, socketChannel);
				acceptConnection(connection);
			}
		} catch (IOException e) {
		}
	}
}
