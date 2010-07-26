/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.zoneproviders;

import java.util.Collection;

import org.xbill.DNS.Zone;

import se.unlogic.eagledns.SecondaryZone;
import se.unlogic.eagledns.plugins.Plugin;

/**
 * 
 * An extension of the {@link Plugin} interface used to create zone providers for Eagle DNS.
 * 
 * This interface is used to dynamically load zones from different type of zone providers in runtime
 * enabling zones to be added, updated and removed in runtime without restarting the Eagle DNS server itself.
 * 
 * @author Unlogic
 *
 */
public interface ZoneProvider extends Plugin{

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
}
