/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import org.xbill.DNS.Zone;

import se.unlogic.eagledns.zoneproviders.ZoneProvider;

public class CachedPrimaryZone {

	protected Zone zone;
	protected ZoneProvider zoneProvider;

	public CachedPrimaryZone(Zone zone, ZoneProvider zoneProvider) {

		super();
		this.zone = zone;
		this.zoneProvider = zoneProvider;
	}

	public Zone getZone() {

		return zone;
	}

	public void setZone(Zone zone) {

		this.zone = zone;
	}

	public ZoneProvider getZoneProvider() {

		return zoneProvider;
	}
}
