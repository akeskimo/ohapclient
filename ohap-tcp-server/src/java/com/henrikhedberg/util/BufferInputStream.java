
/*
 * Miscellaneous Java Utilities by Henrik Hedberg
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

package com.henrikhedberg.util;

import java.io.InputStream;
import java.io.IOException;

/**
 * An {@link InputStream} that contains an internal buffer to enable communication between
 * thread boundaries.
 *
 * <p>A consumer thread uses the standard {@link InputStream} interface
 * (<code>read()</code>, <code>close()</code>) to take buffered bytes.
 *
 * <p>A producer thread sets a handler with the {@link #setHandler(Handler)} method to
 * read the buffered bytes from an other {@link InputStream} with the
 * {@link #readFrom(InputStream)} method. The {@link #end()} method may be used to
 * mark the end of file (EOF).
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20150503)
 */
public class BufferInputStream extends InputStream {
	private byte[] bytes;
	private int position;
	private int readable;
	private boolean ended;
	private Handler handler;
	private boolean closed;

	/**
	 * Constructs a new {@link BufferOutputStream} with the given buffer size.
	 * The buffer does not grow during operation.
	 *
	 * @param bufferSize initial buffer size
	 */
	public BufferInputStream(int bufferSize) {
		bytes = new byte[bufferSize];
	}

	/**
	 * Returns the amount of bytes in the internal buffer currently.
	 *
	 * @return amount of bytes in the buffer
	 */
	@Override
	public int available() throws IOException {
		if (closed)
			throw new IOException("Stream is closed.");
		return readable;
	}

	/**
	 * Closes the stream. The {@link Handler} is notified.
	 */
	@Override
	public synchronized void close() throws IOException {
		if (closed)
			throw new IOException("Stream is closed.");

		closed = true;
		notify();
	
		if (handler != null)
			handler.handleClose(this);
	}
	
	/**
	 * Marks that the end of file (EOF) has been reached.
	 */
	public synchronized void end() throws IOException {
		ended = true;
		notify();
	}
	/**
	 * Reads a byte from the internal buffer.
	 */
	@Override
	public synchronized int read() throws IOException {
		if (closed)
			throw new IOException("Stream is closed.");

		if (readable == 0) {
			if (ended)
				return -1;
			if (handler != null)
				handler.handleRead(this);
			while (readable == 0 && !ended && !closed) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
			if (closed)
				throw new IOException("Stream is closed.");
			if (ended)
				return -1;
		}

		int value = bytes[position];
		position = (position + 1) % bytes.length;
		readable--;

		return value;
	}
	
	/**
	 * Reads bytes from the internal buffer.
	 */
	@Override
	public synchronized int read(byte[] b, int offset, int length) throws IOException {
		if (closed)
			throw new IOException("Stream is closed.");

		if (length == 0)
			return 0;

		if (readable == 0) {
			if (ended)
				return -1;
			if (handler != null)
				handler.handleRead(this);
			while (readable == 0 && !ended && !closed) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
			if (closed)
				throw new IOException("Stream is closed.");
			if (ended)
				return -1;
		}
		
		int give = Math.min(length, readable);
		System.arraycopy(bytes, position, b, offset, give);
		position = (position + give) % bytes.length;
		readable -= give;

		return give;
	}

	/**
	 * Reads bytes from the given {@link InputStream} into the internal buffer.
	 *
	 * @param input a stream to read the bytes into the internal buffer
	 * @return the amount of bytes read
	 */
	public synchronized int readFrom(InputStream input) throws IOException {
		int result = 0;
		
		for (int available = input.available(); available > 0; available = input.available()) {
			int offset = (position + readable) % bytes.length;
			int length = offset < position ? position - offset : bytes.length - offset;
			length = Math.min(length, available);
			int got = input.read(bytes, offset, length);
			if (got > 0) {
				readable += got;
				result += got;
			} else if (got == -1 && result == 0)
				return -1;
		}
		if (result > 0)
			notify();
		
		return result;
	}
	
	/**
	 * Sets a {@link Handler} to produce bytes into the buffer.
	 *
 	 * @param handler a {@link Handler} to handle this stream
	 */
	public synchronized void setHandler(Handler handler) {
		this.handler = handler;
	}
	
	/**
	 * A handler that is called when the specified {@link BufferInputStream} has
	 * run out of bytes or has been asked to close itself.
	 *
	 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
	 * @version 1.0 (20150503)
	 */
	public static interface Handler {
		/**
		 * Called when the specified {@link BufferInputStream} has run out of
		 * bytes.
		 *
		 * @param inputStream the {@link BufferInputStream} to handle
		 */
		public void handleRead(BufferInputStream inputStream) throws IOException;

		/**
		 * Called when the specified {@link BufferInputStream} has been asked
		 * to close itself.
		 *
		 * @param inputStream the {@link BufferInputStream} to handle
		 */
		public void handleClose(BufferInputStream inputStream) throws IOException;
	}
}
