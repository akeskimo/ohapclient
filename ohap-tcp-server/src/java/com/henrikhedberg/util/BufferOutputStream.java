
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

import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * An {@link OutputStream} that contains an internal buffer to enable communication between
 * thread boundaries.
 *
 * <p>A producer thread uses the standard {@link OutputStream} interface
 * (<code>write()</code>, <code>close()</code>) to buffer bytes.
 *
 * <p>A consumer thread sets a handler with the {@link #setHandler(Handler)} method to
 * write the buffered bytes into an other {@link OutputStream} with the
 * {@link #writeTo(OutputStream)} method.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.0 (20150503)
 */
public class BufferOutputStream extends OutputStream {
	private byte[] bytes;
	private int written;
	private Handler handler;
	private boolean closed;

	/**
	 * Constructs a new {@link BufferOutputStream} with the given initial
	 * internal buffer size. The buffer grows when needed.
	 *
	 * @param initialBufferSize initial buffer size
	 */
	public BufferOutputStream(int initialBufferSize) {
		bytes = new byte[initialBufferSize];
	}

	/**
	 * Returns the amount of bytes in the internal buffer currently.
	 *
	 * @return amount of bytes in the buffer
	 */
	public int available() throws IOException {
		return written;
	}

	/**
	 * Closes the stream. The {@link Handler} is notified.
	 */
	@Override
	public synchronized void close() throws IOException {
		if (closed)
			throw new IOException("Stream is closed.");

		closed = true;
		
		if (handler != null)
			handler.handleClose(this);	
	}
	
	/**
	 * Sets a {@link Handler} to consume bytes from the internal buffer.
	 *
	 * @param handler a {@link Handler} to handle this stream
	 */
	public synchronized void setHandler(Handler handler) {
		this.handler = handler;
	}

	/**
	 * Writes the given bytes into the internal buffer.
	 */
	@Override
	public synchronized void write(byte[] b, int offset, int length) throws IOException {
		if (closed)
			throw new IOException("Stream is closed.");

		ensureCapacity(length);
		System.arraycopy(b, offset, bytes, written, length);
		written += length;
		
		if (handler != null)
			handler.handleWrite(this);
	}

	/**
	 * Writes the given byte into the internal buffer.
	 */
	@Override
	public synchronized void write(int b) throws IOException {
		if (closed)
			throw new IOException("Stream is closed.");

		ensureCapacity(1);
		bytes[written++] = (byte)b;

		if (handler != null)
			handler.handleWrite(this);
	}

	/**
	 * Writes the bytes from the internal buffer into the given
	 * {@link OutputStream}. The internal buffer will be empty after
	 * the operation.
	 *
	 * @param output a stream to write the bytes from the internal buffer
	 * @return the amount of bytes written
	 */
	public synchronized int writeTo(OutputStream output) throws IOException {
		if (written == 0 && closed)
			return -1;

		int result = written;
		output.write(bytes, 0, written);
		written = 0;
		notify();
		
		return result;
	}
	
	private void ensureCapacity(int appendLength) {
		if (written + appendLength < bytes.length)
			return;

		int newLength = 2 * bytes.length;
		while (written + appendLength >= bytes.length)
			newLength *= 2;

		bytes = Arrays.copyOf(bytes, newLength);
	}

	/**
	 * A handler that is called when the specified {@link BufferOutputStream} has
	 * buffered new bytes or has been asked to close itself.
	 *
	 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
	 * @version 1.0 (20150503)
	 */
	public static interface Handler {
		/**
		 * Called when the specified {@link BufferOutputStream} has buffered
		 * new bytes.
		 *
		 * @param outputStream the {@link BufferOutputStream} to handle
		 */
		public void handleWrite(BufferOutputStream outputStream) throws IOException;

		/**
		 * Called when the specified {@link BufferOutputStream} has been asked
		 * to close itself.
		 *
		 * @param outputStream the {@link BufferOutputStream} to handle
		 */
		public void handleClose(BufferOutputStream outputStream) throws IOException;
	}
}
