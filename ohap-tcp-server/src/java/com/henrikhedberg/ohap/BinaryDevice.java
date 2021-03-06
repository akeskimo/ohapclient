
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
 * A concrete device with a binary presentation.
 * Inherits all common properties from the {@link Device} super class.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20160311)
 */
public class BinaryDevice extends Device {	
	private boolean value;

	public BinaryDevice(long identifier, String name, String description, boolean internal, Type type, boolean value) {
		super(identifier, name, description, internal, type);

		this.value = value;
	}

	public void outputMessage(OutgoingMessage outgoingMessage) {
		outgoingMessage.integer8(type == Type.ACTUATOR ? OhapServer.MESSAGE_TYPE_BINARY_ACTUATOR : OhapServer.MESSAGE_TYPE_BINARY_SENSOR);
		outputIdentifier(outgoingMessage);
		outgoingMessage.binary8(value);
		outputData(outgoingMessage);	
	}
	
	public void changeValue(boolean value) {
		this.value = value;

		OutgoingMessage outgoingMessage = new OutgoingMessage();
		outgoingMessage.integer8(OhapServer.MESSAGE_TYPE_BINARY_CHANGED);
		outputIdentifier(outgoingMessage);
		outgoingMessage.binary8(value);

		getParent().sendToListeners(outgoingMessage);
	}
}
