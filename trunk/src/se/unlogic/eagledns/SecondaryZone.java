package se.unlogic.eagledns;

import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Zone;

/**
 * This class is used to hold data secondary zones when they are loaded from
 * ZoneProviders. The actual Zone field may be left blank if the ZoneProvider
 * has no previously stored copy of the zoneBackup.
 * 
 * @author Robert "Unlogic" Olofsson
 * 
 */
public class SecondaryZone {

	private Name zoneName;
	private String remoteServerAddress;
	private Zone zoneBackup;

	public SecondaryZone(String zoneName, String remoteServerAddress) throws TextParseException {

		super();
		this.zoneName = Name.fromString(zoneName, Name.root);
		this.remoteServerAddress = remoteServerAddress;
	}

	public SecondaryZone(String zoneName, String remoteServerAddress, Zone zone) throws TextParseException {

		this.zoneName = Name.fromString(zoneName, Name.root);
		this.remoteServerAddress = remoteServerAddress;
		this.zoneBackup = zone;
	}

	public Name getZoneName() {

		return zoneName;
	}

	public void setZoneName(Name zoneName) {

		this.zoneName = zoneName;
	}

	public String getRemoteServerAddress() {

		return remoteServerAddress;
	}

	public void setRemoteServerAddress(String remoteServerIP) {

		this.remoteServerAddress = remoteServerIP;
	}

	public Zone getZoneBackup() {

		return zoneBackup;
	}

	public void setZoneBackup(Zone zone) {

		this.zoneBackup = zone;
	}
}
