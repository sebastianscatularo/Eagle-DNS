package se.unlogic.eagledns.zoneproviders.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Zone;

import se.unlogic.eagledns.SecondaryZone;
import se.unlogic.eagledns.ZoneProvider;


/**
 * This class loads primary zones from zone files in the file system.
 * The zone files have to formated accordingly to RFC 1035 (http://tools.ietf.org/html/rfc1035)
 * and RFC 1034 (http://tools.ietf.org/html/rfc1034).
 * 
 * @author Robert "Unlogic" Olofsson
 *
 */
public class FileZoneProvider implements ZoneProvider {

	private final Logger log = Logger.getLogger(this.getClass());

	private String name;
	private String zoneFileDirectory;

	public void init(String name) {

		this.name = name;

		// TODO Logging
	}

	public Collection<Zone> getPrimaryZones() {

		File zoneDir = new File(this.zoneFileDirectory);

		if(!zoneDir.exists() || !zoneDir.isDirectory()){

			log.error("Zone file directory specified for FileZoneProvider " + name + " does not exist!");
			return null;

		}else if(!zoneDir.canRead()){

			log.error("Zone file directory specified for FileZoneProvider " + name + " is not readable!");
			return null;
		}

		File[] files = zoneDir.listFiles();

		if(files == null || files.length == 0){

			log.info("No zone files found for FileZoneProvider " + name + " in directory " + zoneDir.getPath());
			return null;
		}

		ArrayList<Zone> zones = new ArrayList<Zone>(files.length);

		for(File zoneFile : files){

			if(!zoneFile.canRead()){
				log.error("FileZoneProvider " + name + " unable to access zone file " + zoneFile );
				continue;
			}

			Name origin;
			try {

				origin = Name.fromString(zoneFile.getName(), Name.root);
				Zone zone = new Zone(origin, zoneFile.getPath());

				log.debug("FileZoneProvider " + name + " successfully parsed zone file " + zoneFile.getName());

				zones.add(zone);

			} catch (TextParseException e) {

				log.error("FileZoneProvider " + name + " unable to parse zone file " + zoneFile.getName(),e);

			} catch (IOException e) {

				log.error("Unable to parse zone file " + zoneFile + " in FileZoneProvider " + name,e);
			}
		}

		if(!zones.isEmpty()){

			return zones;
		}

		return null;
	}

	public void unload() {

	}


	public String getZoneFileDirectory() {
		return zoneFileDirectory;
	}


	public void setZoneFileDirectory(String zoneFileDirectory) {

		this.zoneFileDirectory = zoneFileDirectory;

		log.debug("zoneFileDirectory set to " + zoneFileDirectory);
	}

	public Collection<SecondaryZone> getSecondayZones() {

		//Not supported
		return null;
	}

	public void zoneUpdated(SecondaryZone secondaryZone) {

		//Not supported
	}
}
