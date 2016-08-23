
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

import java.nio.channels.*;
import java.util.Iterator;
import java.io.IOException;

/**
 * A loop around the {@link Selector}. Handlers for accepting, connecting, reading and writing
 * may be registered for {@link SelectableChannel}s. 
 */
public class SelectorLoop implements Runnable {
	private Selector selector;
	private boolean running = true;
	private IOException exception = null;
	
	/**
	 * Constructs a new SelectorLoop.
	 */
	public SelectorLoop() throws IOException {
		selector = Selector.open();
	}
	
	public void registerAcceptableHandler(SelectableChannel channel, AcceptableHandler handler) throws ClosedChannelException {
		HandlerData handlerData = getHandlerData(channel, handler != null, SelectionKey.OP_ACCEPT);
		handlerData.acceptableHandler = handler;
	}
	
	public void registerConnectableHandler(SelectableChannel channel, ConnectableHandler handler) throws ClosedChannelException {
		HandlerData handlerData = getHandlerData(channel, handler != null, SelectionKey.OP_CONNECT);
		handlerData.connectableHandler = handler;
	}
	
	public void registerReadableHandler(SelectableChannel channel, ReadableHandler handler) throws ClosedChannelException {
		HandlerData handlerData = getHandlerData(channel, handler != null, SelectionKey.OP_READ);
		handlerData.readableHandler = handler;
	}
	
	public void registerWritableHandler(SelectableChannel channel, WritableHandler handler) throws ClosedChannelException {
		HandlerData handlerData = getHandlerData(channel, handler != null, SelectionKey.OP_WRITE);
		handlerData.writableHandler = handler;
	}
	
	/**
	 * Clears and returns the {@link IOException} caught in the {@link #run()} loop.
	 *
	 * @return caught exception or null if no exception
	 */
	public IOException handleException() {
		IOException exception = this.exception;
		this.exception = null;
		return exception;
	}

	/**
	 * Runs the loop.
	 *
	 * If an IOException is caught, the loop is interrupted and the exception is saved.
	 * The loop cannot be restarted before the exception is cleared with
	 * {@link #handleException()}.
	 */
	public void run() {
		if (exception != null)
			return;

		try {
			while (running) {
				if (selector.select() > 0) {
					Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
					while(iterator.hasNext()) {
						SelectionKey selectionKey = iterator.next();
						HandlerData handlerData = (HandlerData)selectionKey.attachment();
						if (selectionKey.isValid() && selectionKey.isAcceptable())
							handlerData.acceptableHandler.handleAcceptable(selectionKey.channel());
						if (selectionKey.isValid() && selectionKey.isConnectable())
							handlerData.connectableHandler.handleConnectable(selectionKey.channel());
						if (selectionKey.isValid() && selectionKey.isReadable())
							handlerData.readableHandler.handleReadable(selectionKey.channel());
						if (selectionKey.isValid() && selectionKey.isWritable())
							handlerData.writableHandler.handleWritable(selectionKey.channel());
						iterator.remove();
					}
				}
			}
		} catch (IOException e) {
			exception = null;
		}
	}

	/**
	 * Stops the loop. It may be restarted again.
	 */
	public void stop() {
		running = false;
		selector.wakeup();
	}

	private HandlerData getHandlerData(SelectableChannel channel, boolean setInterest, int interest) throws ClosedChannelException{
		HandlerData handlerData;
		int interestOps = 0;
		
		SelectionKey selectionKey = channel.keyFor(selector);
		if (selectionKey != null) {
			handlerData = (HandlerData)selectionKey.attachment();
			interestOps = selectionKey.interestOps();
		} else
			handlerData = new HandlerData();
		
		if (setInterest)
			interestOps |= interest;
		else
			interestOps &= ~interest;
		
		channel.register(selector, interestOps, handlerData);
		
		return handlerData;	
	}
	
	public static interface AcceptableHandler {
		public void handleAcceptable(SelectableChannel channel);
	}

	public static interface ConnectableHandler {
		public void handleConnectable(SelectableChannel channel);
	}

	public static interface ReadableHandler {
		public void handleReadable(SelectableChannel channel);
	}

	public static interface WritableHandler {
		public void handleWritable(SelectableChannel channel);
	}

	private static class HandlerData {
		public AcceptableHandler acceptableHandler;
		public ConnectableHandler connectableHandler;
		public ReadableHandler readableHandler;
		public WritableHandler writableHandler;
	}
}
