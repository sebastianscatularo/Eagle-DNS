package se.unlogic.eagledns;

import java.util.Collection;

import org.xbill.DNS.Zone;

/**
 * This interface is used to dynamicly load zones from different type of zone providers in runtime
 * enabling zones to be added, updated and removed in runtime without restarting the EagleDNS dns server itself.
 * 
 * @author Unlogic
 *
 */
public interface ZoneProvider {

	/**
	 * This method is called after the ZoneProvider has been instantiated by EagleDNS and all properties
	 * specified in the config file for this zone provider have been set using their set methods.
	 */
	public void init(String name);


	/**
	 * This method is called each time EagleDNS reloads it's zones.
	 * If no zones are found or if an error occurs the the ZoneProvider should return null.
	 * 
	 * @return
	 */
	public Collection<Zone> getZones();


	/**
	 * This method is called when EagleDNS is shutdown or when the configuration has been updated and
	 * the ZoneProvider is no longer present in the configuration file.
	 */
	public void unload();
}
