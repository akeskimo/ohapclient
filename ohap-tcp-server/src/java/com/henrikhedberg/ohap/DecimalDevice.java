
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
 * A concrete device with a decimal presentation.
 * Inherits all common properties from the {@link Device} super class.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20160311)
 */
public class DecimalDevice extends Device {
	private double value;
	private double min;
	private double max;
	private String unit;
	private String unitAbbreviation;

	public DecimalDevice(long identifier, String name, String description, boolean internal, Type type, double value, double min, double max, String unit, String unitAbbreviation) {
		super(identifier, name, description, internal, type);

		this.value = value;
		this.min = min;
		this.max = max;
		this.unit = unit;
		this.unitAbbreviation = unitAbbreviation;
	}

	public void outputMessage(OutgoingMessage outgoingMessage) {
		outgoingMessage.integer8(type == Type.ACTUATOR ? OhapServer.MESSAGE_TYPE_DECIMAL_ACTUATOR : OhapServer.MESSAGE_TYPE_DECIMAL_SENSOR);
		outputIdentifier(outgoingMessage);
		outgoingMessage.decimal64(value);
		outputData(outgoingMessage);
		outgoingMessage.decimal64(min).decimal64(max).text(unit).text(unitAbbreviation);
	}
	
	public void changeValue(double value) {
		this.value = value;

		OutgoingMessage outgoingMessage = new OutgoingMessage();
		outgoingMessage.integer8(OhapServer.MESSAGE_TYPE_DECIMAL_CHANGED);
		outputIdentifier(outgoingMessage);
		outgoingMessage.decimal64(value);

		getParent().sendToListeners(outgoingMessage);
	}
}
