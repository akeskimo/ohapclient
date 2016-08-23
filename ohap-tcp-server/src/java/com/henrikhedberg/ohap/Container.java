
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

import java.util.HashSet;
import java.io.OutputStream;
import java.io.IOException;

/**
 * A container holding items. Inherits all common properties from the
 * {@link Item} base class.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20160311)
 */
public class Container extends Item {
	private OhapServer server;
	private HashSet<Item> items = new HashSet<>();
	private HashSet<OhapSession> listeners = new HashSet<>();

	public Container(long identifier, String name, String description, boolean internal) {
		super(identifier, name, description, internal);
	}

	public void outputMessage(OutgoingMessage outgoingMessage) {
		outgoingMessage.integer8(OhapServer.MESSAGE_TYPE_CONTAINER);
		outputIdentifier(outgoingMessage);
		outputData(outgoingMessage);	
	}

	public void addItem(Item item) {
		item.setParent(this);
		items.add(item);
		if (server != null) {
			item.attachToServer(server);
		}
	}
	
	protected void attachToServer(OhapServer server) {
		super.attachToServer(server);
		this.server = server;
		for (Item item : items)
			item.attachToServer(server);
	}

	public void addListener(OhapSession session) {
		listeners.add(session);
		for (Item item: items) {
			OutgoingMessage outgoingMessage = new OutgoingMessage();
			item.outputMessage(outgoingMessage);
			session.sendMessage(outgoingMessage);
		}
	}
	
	public void removeListener(OhapSession session) {
		listeners.remove(session);
	}
	
	void sendToListeners(OutgoingMessage outgoingMessage) {
		OhapSession[] sessions = listeners.toArray(new OhapSession[listeners.size()]);
		for (OhapSession session : sessions) {
			session.sendMessage(outgoingMessage);
		}
	}
}
