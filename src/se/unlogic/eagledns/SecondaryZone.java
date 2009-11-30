package se.unlogic.eagledns;

import org.xbill.DNS.Zone;

/**
 * This class is used to hold data secondary zones when they are loaded from
 * ZoneProviders. The actual Zone field may be left blank if the ZoneProvider
 * has no previously stored copy of the zone.
 * 
 * @author Robert "Unlogic" Olofsson
 * 
 */
public class SecondaryZone {

	private String zoneName;
	private String remoteServerIP;
	private Zone zone;

	public SecondaryZone(String zoneName, String remoteServerIP) {

		super();
		this.zoneName = zoneName;
		this.remoteServerIP = remoteServerIP;
	}

	public SecondaryZone(String zoneName, String remoteServerIP, Zone zone) {

		this.zoneName = zoneName;
		this.remoteServerIP = remoteServerIP;
		this.zone = zone;
	}

	public String getZoneName() {

		return zoneName;
	}

	public void setZoneName(String zoneName) {

		this.zoneName = zoneName;
	}

	public String getRemoteServerIP() {

		return remoteServerIP;
	}

	public void setRemoteServerIP(String remoteServerIP) {

		this.remoteServerIP = remoteServerIP;
	}

	public Zone getZone() {

		return zone;
	}

	public void setZone(Zone zone) {

		this.zone = zone;
	}
}
