
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
 * An abstract device that an be either an actuator or a sensor.
 * Inherits all common properties from the {@link Item} base class.
 *
 * @author Henrik Hedberg &lt;henrik.hedberg@iki.fi&gt;
 * @version 1.1 (20160311)
 */
public abstract class Device extends Item {
	public enum Type {
		ACTUATOR,
		SENSOR
	}
	
	protected Type type;

	public Device(long identifier, String name, String description, boolean internal, Type type) {
		super(identifier, name, description, internal);

		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
}
