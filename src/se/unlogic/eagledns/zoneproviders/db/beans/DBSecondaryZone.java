/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.zoneproviders.db.beans;

import org.xbill.DNS.TextParseException;

import se.unlogic.eagledns.SecondaryZone;


public class DBSecondaryZone extends SecondaryZone {

	private Integer zoneID;
	
	public DBSecondaryZone(Integer zoneID, String zoneName, String remoteServerAddress, String dclass) throws TextParseException {

		super(zoneName, remoteServerAddress, dclass);
		this.zoneID = zoneID;
	}

	
	public Integer getZoneID() {
	
		return zoneID;
	}
}
