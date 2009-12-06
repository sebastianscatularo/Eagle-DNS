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
	public void init(String name) throws Exception;


	/**
	 * This method is called each time EagleDNS reloads it's zones.
	 * If no zones are found or if an error occurs the the ZoneProvider should return null
	 * else it should return all primary zones available from the zone provider.
	 * 
	 * @return
	 */
	public Collection<Zone> getPrimaryZones();

	/**
	 * This method is called each time EagleDNS reloads it's zones.
	 * If no zones are found or if an error occurs the the ZoneProvider should return null
	 * else it should return all secondary zones available from the zone provider.
	 * 
	 * The returned secondary zones may contain a previously saved copy of the zone if the ZoneProvider supports this feature.
	 * 
	 * @return
	 */
	public Collection<SecondaryZone> getSecondaryZones();

	/**
	 * This method is called when a change has been detected in a secondary zone previously
	 * loaded from this ZoneProvider. Failed AXFR requests will not trigger this method, although zone expiry will.
	 * 
	 * The main purpose of this method is to enable the ZoneProviders to save the updated
	 * zone data which is useful in case EagleDNS is restarted when the primary DNS server of the zone is down.
	 * 
	 * @param zone
	 */
	public void zoneUpdated(SecondaryZone secondaryZone);


	/**
	 * This method is called each time a zone has been downloaded and no changes have been detected (by comparing the serial)
	 * 
	 * @param secondaryZone
	 */
	public void zoneChecked(SecondaryZone secondaryZone);

	/**
	 * This method is called when EagleDNS is shutdown or when the configuration has been updated and
	 * the ZoneProvider is no longer present in the configuration file.
	 */
	public void unload();
}
