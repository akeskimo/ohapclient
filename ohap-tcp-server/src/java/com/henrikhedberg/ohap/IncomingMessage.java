
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

/**
 * Parses an incoming OHAP message.
 *
 * <p>Call either {@link #readFrom(InputStream)} or {@link #readFromNB(InputStream)} to
 * read a message from an {@link InputStream} or @{link #readFrom(ByteBuffer) to read
 * a message from a {@link ByteBuffer}. Then, use {@link #integer8()},
 * {@link #integer16()}, {@link #integer32()}, {@link #decimal64()}, {@link #allBytes(byte[])},
 * {@link #binary8()}, and {@link #text()} sequentially to take parsed values.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20160320)
 */
public class IncomingMessage {
	private byte[] buffer;
	private int position;
	private int nbLength = -1;
	private final Charset charset = Charset.forName("UTF-8");

	/**
	 * Reads one message from the given {@link InputStream}. Blocks until
	 * the message is fully read.
	 *
	 * @param inputStream the stream to read from
	 * @throws IOException if an operation on the given stream throws an exception
	 */
	public void readFrom(InputStream inputStream) throws IOException {
		buffer = readExactly(inputStream, 2);
		position = 0;
		int length = integer16();

		buffer = readExactly(inputStream, length);
		position = 0;
	}

	/**
	 * Tries to read one message from the given {@link InputStream}. If
	 * the message is fully read, returns true. If the message is not
	 * fully available yet, does not block but returns false. In that
	 * case, the message may be partially read from the stream.
	 *
	 * <p>Relies on the {@link InputStream#available()} to work.
	 *
	 * @param inputStream the stream to read from
	 * @return whether the message was fully read
	 * @throws IOException if an operation on the given stream throws an exception
	 */
	public boolean readFromNB(InputStream inputStream) throws IOException {
		if (nbLength == -1) {
			if (inputStream.available() < 2)
				return false;

			buffer = readExactly(inputStream, 2);
			position = 0;
			nbLength = integer16();
		}
	
		if (inputStream.available() < nbLength)
			return false;

		buffer = readExactly(inputStream, nbLength);
		position = 0;
		nbLength = -1;

		return true;
	}

	/**
	 * Tries to read one message from the given {@link ByteBuffer}. If
	 * the message is fully read, returns true. If the message is not
	 * fully available yet, does not block but returns false. In that
	 * case, the message may be partially taken from the buffer.
	 *
	 * @param byteBuffer the buffer to read from
	 * @return whether the message was fully read
	 */
	public boolean readFromNB(ByteBuffer byteBuffer) {
		if (nbLength == -1) {
			if (byteBuffer.remaining() < 2)
				return false;

			buffer = readExactly(byteBuffer, 2);
			position = 0;
			nbLength = integer16();
		}

		if (byteBuffer.remaining() < nbLength)
			return false;

		buffer = readExactly(byteBuffer, nbLength);
		position = 0;
		nbLength = -1;

		return true;
	}

	/**
	 * Takes the next unsigned 8-bit integer from the message.
	 *
	 * @return unsigned 8-bit number
	 * @throws ArrayIndexOutOfBoundsException if there are not enough bytes left
	 */
	public int integer8() {
		if (position + 1 > buffer.length)
			throw new ArrayIndexOutOfBoundsException();

		int value = buffer[position] & 0xff;
		position += 1;

		return value;
	}

	/**
	 * Takes the next unsigned 16-bit integer from the message.
	 *
	 * @return unsigned 16-bit integer
	 * @throws ArrayIndexOutOfBoundsException if there are not enough bytes left
	 */
	public int integer16() {
		if (position + 2 > buffer.length)
			throw new ArrayIndexOutOfBoundsException();

		int value = (buffer[position] & 0xff) << 8 |
					(buffer[position + 1] & 0xff);
		position += 2;

		return value;
	}

	/**
	 * Takes the next unsigned 32-bit integer from the message.
	 *
	 * @return unsigned 32-bit integer
	 * @throws ArrayIndexOutOfBoundsException if there are not enough bytes left
	 */
	public long integer32() {
		if (position + 4 > buffer.length)
			throw new ArrayIndexOutOfBoundsException();

		long value = (buffer[position] & 0xffL) << 24 |
					 (buffer[position + 1] & 0xffL) << 16 |
					 (buffer[position + 2] & 0xffL) << 8 |
					 (buffer[position + 3] &0xffL);
		position += 4;

		return value;
	}

	/**
	 * Takes the next IEEE 754 floating-point double precision decimal number from the message.
	 *
	 * @return IEEE 754 floating-point double precision decimal number
	 * @throws ArrayIndexOutOfBoundsException if there are not enough bytes left
	 */
	public double decimal64() {
		if (position + 8 > buffer.length)
			throw new ArrayIndexOutOfBoundsException();

		long value = (buffer[position] & 0xffL) << 56 |
				(buffer[position + 1] & 0xffL) << 48 |
				(buffer[position + 2] & 0xffL) << 40 |
				(buffer[position + 3] & 0xffL) << 32 |
				(buffer[position + 4] & 0xffL) << 24 |
				(buffer[position + 5] & 0xffL) << 16 |
				(buffer[position + 6] & 0xffL) << 8 |
				(buffer[position + 7] &0xffL);
		position += 8;

		return Double.longBitsToDouble(value);
	}

	/**
	 * Takes the next <code>n</code> bytes from the message, where
	 * the <code>n</code> is the size of the given array.
	 *
	 * @param bytes an array to store bytes
	 * @throws ArrayIndexOutOfBoundsException if there are not enough bytes left
	 */
	public void allBytes(byte[] bytes) {
		if (position + bytes.length > buffer.length)
			throw new ArrayIndexOutOfBoundsException();

		System.arraycopy(buffer, position, bytes, 0, bytes.length);
		position += bytes.length;
	}

	/**
	 * Takes the next byte from the message and converts it to a boolean value.
	 *
	 * @return boolean value
	 * @throws ArrayIndexOutOfBoundsException if there are not enough bytes left
	 */
	public boolean binary8() {
		int i = integer8();
		if (i != 0 && i != 1)
			throw new IllegalStateException("The byte was not binary.");
		return i == 1;
	}

	/**
	 * Treats the next bytes as text and takes it from the message.
	 *
	 * @return text
	 * @throws ArrayIndexOutOfBoundsException if there are not enough bytes left
	 */
	public String text() {
		int length = integer16();
		byte[] bytes = new byte[length];
		allBytes(bytes);

		return new String(bytes, charset);
	}

	private static byte[] readExactly(InputStream inputStream, int length) throws IOException {
		byte[] bytes = new byte[length];

		int offset = 0;
		while (length > 0) {
			int got = inputStream.read(bytes, offset, length);
			if (got == -1)
				throw new EOFException("End of message input.");
			offset += got;
			length -= got;
		}

		return bytes;
	}

	private static byte[] readExactly(ByteBuffer byteBuffer, int length) {
		byte[] bytes = new byte[length];

		byteBuffer.get(bytes);

		return bytes;
	}
}
