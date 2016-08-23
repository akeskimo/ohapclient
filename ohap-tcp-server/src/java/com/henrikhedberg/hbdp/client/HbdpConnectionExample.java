
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

package com.henrikhedberg.hbdp.client;

import com.henrikhedberg.hbdp.client.HbdpConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A program that demonstrates the simplest usage of the {@link HbdpConnection} class.
 *
 * <p>The main method constructs an {@link HbdpConnection} object with a specified
 * URL. Then it spawns a separate thread to read characters from the connection and to 
 * write those into the standard output. The main thread reads the standard input and
 * writes those into the connection. When the end of the standard input stream is
 * found, the connection is closed.
 *
 * <p><pre>
 * {@code
 * final HbdpConnection hbdp = new HbdpConnection(new URL(args[0]));
 * new Thread() {
 * 	public void run() {
 * 		cat(hbdp.getInputStream(), System.out);
 * 	}
 * }.start();
 *
 * cat(System.in, hbdp.getOutputStream());
 *
 * hbdp.close(); }</pre>
 *
 * <p>The cat() method is defined as follows:
 *
 * <p><pre>
 * {@code
 * private static void cat(InputStream input, OutputStream output) {
 * 	try {
 * 		int c;
 * 		do {
 * 			c = input.read();
 * 			if (c != -1)
 * 				output.write(c);
 * 		} while (c != -1);
 * 	} catch (IOException exception) {
 * 		exception.printStackTrace();
 * 	}
 * } }</pre>
 */

public class HbdpConnectionExample {
	public static void main(String[] args) throws MalformedURLException {
		if (args.length != 1) {
			System.err.println("Usage: java -jar hbdp-connection-example <URL>");
			return;
		}
		final HbdpConnection hbdp = new HbdpConnection(new URL(args[0]));
		new Thread() {
			public void run() {
				cat(hbdp.getInputStream(), System.out);
			}
		}.start();

		cat(System.in, hbdp.getOutputStream());

		hbdp.close();
	}
	
	private static void cat(InputStream input, OutputStream output) {
		try {
			int c;
			do {
				c = input.read();
				if (c != -1)
					output.write(c);
			} while (c != -1);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
}
