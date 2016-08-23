
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.nio.ByteBuffer;

/**
 * Builds an outgoing OHAP message.
 *
 * <p>Use {@link #integer8(int)}, {@link #integer16(int)}, {@link #integer32(long)},
 * {@link #decimal64(double)}, {@link #allBytes(byte[])}, {@link #binary8(boolean)},
 * and {@link #text(String)} sequentially to build a message. Then, call
 * {@link #writeTo(OutputStream)} to write it into an {@link OutputStream}, or
 * {@link #asByteBuffer()} to get it as a @{link ByteBuffer}.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20160320)
 */
public class OutgoingMessage {
	private byte[] buffer = new byte[256];
	private int position = 2;
	private final Charset charset = Charset.forName("UTF-8");

	/**
	 * Appends unsigned 8-bit integer into the message.
	 *
	 * @param value unsigned 8-bit integer
	 * @return itself (for chaining calls)
	 */
	public OutgoingMessage integer8(int value) {
		ensureCapacity(1);
		buffer[position] = (byte)value;
		position += 1;

		return this;
	}

	/**
	 * Appends unsigned 16-bit integer into the message.
	 *
	 * @param value unsigned 16-bit integer
	 * @return itself (for chaining calls)
	 */
	public OutgoingMessage integer16(int value) {
		ensureCapacity(2);
		buffer[position] = (byte)(value >> 8);
		buffer[position + 1] = (byte)value;
		position += 2;

		return this;
	}

	/**
	 * Appends unsigned 32-bit integer into the message.
	 *
	 * @param value unsigned 32-bit integer
	 * @return itself (for chaining calls)
	 */
	public OutgoingMessage integer32(long value) {
		ensureCapacity(4);
		buffer[position] = (byte)(value >> 24);
		buffer[position + 1] = (byte)(value >> 16);
		buffer[position + 2] = (byte)(value >> 8);
		buffer[position + 3] = (byte)value;
		position += 4;

		return this;
	}

	/**
	 * Appends IEEE 754 floating-point double precision decimal number into the message.
	 *
	 * @param value IEEE 754 floating-point double precision decimal number 
	 * @return itself (for chaining calls)
	 */
	public OutgoingMessage decimal64(double value) {
		long bits = Double.doubleToRawLongBits(value);
		ensureCapacity(8);
		buffer[position] = (byte)(bits >> 56);
		buffer[position + 1] = (byte)(bits >> 48);
		buffer[position + 2] = (byte)(bits >> 40);
		buffer[position + 3] = (byte)(bits >> 32);
		buffer[position + 4] = (byte)(bits >> 24);
		buffer[position + 5] = (byte)(bits >> 16);
		buffer[position + 6] = (byte)(bits >> 8);
		buffer[position + 7] = (byte)bits;
		position += 8;

		return this;
	}

	/**
	 * Appends the given bytes into the message.
	 *
	 * @param bytes bytes
	 * @return itself (for chaining calls)
	 */
	public OutgoingMessage allBytes(byte[] bytes) {
		ensureCapacity(bytes.length);
		System.arraycopy(bytes, 0, buffer, position, bytes.length);
		position += bytes.length;

		return this;
	}

	/**
	 * Appends text into the message.
	 *
	 * @param string text
	 * @return itself (for chaining calls)
	 */
	public OutgoingMessage text(String string) {
		byte[] b = string.getBytes(charset);
		integer16(b.length);
		allBytes(b);

		return this;
	}

	/**
	 * Appends boolean value as a byte into the message.
	 *
	 * @param value boolean value
	 * @return itself (for chaining calls)
	 */
	public OutgoingMessage binary8(boolean value) {
		return integer8(value ? 1 : 0);
	}

	/**
	 * Writes the message into the given {@link OutputStream}.
	 *
	 * @param outputStream the stream to write to
	 * @throws IOException if an operation on the given stream throws an exception
	 */
	public void writeTo(OutputStream outputStream) throws IOException {
		int length = position;
		position = 0;
		integer16(length - 2);
		position = length;

		outputStream.write(buffer, 0, length);
	}

	/**
	 * Returns the message as a {@link ByteBuffer}.
	 *
	 * @return message as a ByteBuffer
	 */
	public ByteBuffer asByteBuffer() {
		int length = position;
		position = 0;
		integer16(length - 2);
		position = length;

		return ByteBuffer.wrap(buffer, 0, length);
	}

	private void ensureCapacity(int appendLength) {
		if (position + appendLength < buffer.length)
			return;

		int newLength = buffer.length * 2;
		while (position + appendLength >= newLength)
			newLength *= 2;
		buffer = Arrays.copyOf(buffer, newLength);
	}
}


