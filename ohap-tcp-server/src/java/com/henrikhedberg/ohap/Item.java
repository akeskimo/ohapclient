
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

/**
 * A base object holding all common properties of an item.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20160311)
 */
public abstract class Item {
	private Container parent;
	private long identifier;
	private String name;
	private String description;
	private boolean internal;

	public Item(long identifier, String name, String description, boolean internal) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.internal = internal;
	}

	public long getIdentifier() {
		return identifier;
	}

	public void outputIdentifier(OutgoingMessage outgoingMessage) {
		outgoingMessage.integer32(identifier);
	}

	public abstract void outputMessage(OutgoingMessage outgoingMessage);

	public void outputData(OutgoingMessage outgoingMessage) {
		if (parent != null)
			parent.outputIdentifier(outgoingMessage);
		else
			outgoingMessage.integer32(0);
		outgoingMessage.text(name)
			       .text(description)
			       .binary8(internal);
	}
	
	public Container getParent() {
		return parent;
	}
	
	void setParent(Container parent) {
		if (parent != null && this.parent != null)
			throw new IllegalStateException("Item has already parent.");

		this.parent = parent;
	}

	protected void attachToServer(OhapServer server) {
		server.addItem(this);
	}
}
