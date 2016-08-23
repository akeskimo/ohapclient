	
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

import com.henrikhedberg.hbdp.server.*;
import java.util.HashSet;
import java.io.IOException;

/**
 * Open Home Automation Protocol (OHAP) server-side session.
 *
 * <p>The {@link OhapServer} instantiates a session for
 * each incoming {@link HbdpConnection}. The session tracks
 * the listening state of its client and sends required update
 * messages through a listener mechanism.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20150312)
 */
public abstract class OhapSession {
	private OhapServer server;
	private String identifier;
	private String user;
	private HashSet<Container> listenedContainers = new HashSet<>();
	
	OhapSession(OhapServer server, String identifier) {
		this.server = server;
		this.identifier = identifier;
	}
	
	protected abstract void writeMessage(OutgoingMessage outgoingMessage) throws IOException;
	protected abstract void close() throws IOException;

	public void sendMessage(OutgoingMessage outgoingMessage) {
		try {
			writeMessage(outgoingMessage);
		} catch (IOException e) {
			log("Error: IOException when writing: " + e.getMessage());
			end();
			// TODO: Remove itself from somewhere?
		}
	}
	
	protected void readMessageFailed(IOException exception) {
		log("Error: IOException when reading: " + exception.getMessage());
		end();
	}
	
	protected void handleMessage(IncomingMessage incomingMessage) {
		try {
			int type = incomingMessage.integer8();
			if (user == null && type != OhapServer.MESSAGE_TYPE_LOGIN) {
				log("Error: First message not login");
				end();
				return;
			}
			switch (type) {
				case OhapServer.MESSAGE_TYPE_LOGOUT:
					log("Message: Logout");
					end();
					break;
				case OhapServer.MESSAGE_TYPE_LOGIN:
					handleLogin(incomingMessage);
					break;
				case OhapServer.MESSAGE_TYPE_PING:
					handlePing(incomingMessage);
					break;
				case OhapServer.MESSAGE_TYPE_PONG:
					handlePong(incomingMessage);
					break;
				case OhapServer.MESSAGE_TYPE_DECIMAL_CHANGED:
					handleDecimalChanged(incomingMessage);
					break;
				case OhapServer.MESSAGE_TYPE_BINARY_CHANGED:
					handleBinaryChanged(incomingMessage);
					break;
				case OhapServer.MESSAGE_TYPE_LISTENING_START:
					handleListening(incomingMessage, true);
					break;
				case OhapServer.MESSAGE_TYPE_LISTENING_STOP:
					handleListening(incomingMessage, false);
					break;
				default:
					sendError("Wrong message type: " + type);
					break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			sendError("Malformed message");
		}
	}
	
	private void end() {
		try {
			close();
		} catch (IOException e) {
		}
		
		for (Container container : listenedContainers) {
			container.removeListener(this);
		}
		listenedContainers.clear();	
	}

	private void handleLogin(IncomingMessage incomingMessage) {
		int protocolVersion = incomingMessage.integer8();
		String name = incomingMessage.text();
		String password = incomingMessage.text();

		if (protocolVersion != 1) {
			sendError("Wrong protocol version: " + protocolVersion + ", only 1 is supported");
			return;
		}		
		if (user != null) {
			sendError("Already logged in");
			return;
		}
		if (!server.authenticateUser(name, password)) {
			sendError("Login failed: " + name);
			return;
		}
		
		log("Login: "+ name);
		user = name;
		
		OutgoingMessage outgoingMessage = new OutgoingMessage();
		Item rootContainer = server.getItemByIdentifier(0);
		rootContainer.outputMessage(outgoingMessage);
		sendMessage(outgoingMessage);
	}

	private void handlePing(IncomingMessage incomingMessage) {
		long pingIdentifier = incomingMessage.integer32();

		log("Ping: " + pingIdentifier);

		OutgoingMessage outgoingMessage = new OutgoingMessage();
		outgoingMessage.integer8(OhapServer.MESSAGE_TYPE_PONG).integer32(pingIdentifier);
		sendMessage(outgoingMessage);
	}
	
	private void handlePong(IncomingMessage incomingMessage) {
		long pingIdentifier = incomingMessage.integer32();

		log("Pong: " + pingIdentifier);
	}

	private void handleDecimalChanged(IncomingMessage incomingMessage) {
		long itemIdentifier = incomingMessage.integer32();
		double decimalValue = incomingMessage.decimal64();

		Item item = server.getItemByIdentifier(itemIdentifier);
		if (item == null) {
			sendError("No such item: " + itemIdentifier);
			return;
		}
		if (!(item instanceof DecimalDevice)) {
			sendError("Item is not decimal actuator: " + itemIdentifier);
			return;
		}
		
		DecimalDevice device = (DecimalDevice)item;
		if (device.getType() != Device.Type.ACTUATOR) {
			sendError("Item is not decimal actuator: " + itemIdentifier);
			return;
		}

		log("Change: " + device.getIdentifier() + " -> " + decimalValue);
		device.changeValue(decimalValue);
	}

	private void handleBinaryChanged(IncomingMessage incomingMessage) {
		long itemIdentifier = incomingMessage.integer32();
		boolean binaryValue = incomingMessage.binary8();

		Item item = server.getItemByIdentifier(itemIdentifier);
		if (item == null) {
			sendError("No such item: " + itemIdentifier);
			return;
		}
		if (!(item instanceof BinaryDevice)) {
			sendError("Item is not binary actuator: " + itemIdentifier);
			return;
		}
		
		BinaryDevice device = (BinaryDevice)item;
		if (device.getType() != Device.Type.ACTUATOR) {
			sendError("Item is not binary actuator: " + itemIdentifier);
			return;
		}

		log("Change: " + device.getIdentifier() + " -> " + binaryValue);
		device.changeValue(binaryValue);
	}

	private void handleListening(IncomingMessage incomingMessage, boolean start) {
		long itemIdentifier = incomingMessage.integer32();
		
		Item item = server.getItemByIdentifier(itemIdentifier);
		if (item == null) {
			sendError("No such item: " + itemIdentifier);
			return;
		}
		if (!(item instanceof Container)) {
			sendError("Item is not container: " + itemIdentifier);
			return;
		}
		
		Container container = (Container)item;
		if (start) {
			if (listenedContainers.add(container)) {
				log("Start listening: " + container.getIdentifier());
				container.addListener(this);
			} else
				sendError("Container was already being listened: " + itemIdentifier);
		} else {
			if (listenedContainers.remove(container)) {
				log("Stop listening: " + container.getIdentifier());
				container.removeListener(this);
			} else
				sendError("Container was not being listened: " + itemIdentifier);
		}
	}
	private void sendError(String message) {
		log("Error: " + message);
		OutgoingMessage outgoingMessage = new OutgoingMessage();
		outgoingMessage.integer8(OhapServer.MESSAGE_TYPE_LOGOUT).text(message);
		sendMessage(outgoingMessage);
		
		end();
	}

	private void log(String detail) {
		long seconds = System.currentTimeMillis() / 1000;
		System.out.println(seconds + "  " + identifier + "  Ohap  " + detail);
	}
}
