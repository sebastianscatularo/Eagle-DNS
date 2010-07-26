/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.xbill.DNS.Name;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.Zone;

import se.unlogic.eagledns.plugins.Plugin;
import se.unlogic.eagledns.resolvers.Resolver;
import se.unlogic.eagledns.zoneproviders.ZoneProvider;


/**
 * This interface is used by {@link Plugin}'s to access the internal functions of Eagle DNS
 * 
 * @author Robert "Unlogic" Olofsson (unlogic@unlogic.se)
 *
 */
public interface SystemInterface {

	/**
	 * This method is used to retrieve zones from Eagle DNS internal cache of authorative zones loaded thru the {@link ZoneProvider}'s.
	 * 
	 * @param name of the zone
	 * @return {@link Zone} the requested zone or null if no matching zone was found
	 */
	public Zone getZone(Name name);
	
	public TSIG getTSIG(Name name);

	public void shutdown();

	public void reloadZones();

	public String getVersion();

	public long getStartTime();

	public int getResolverCount();

	public int secondaryZoneCount();

	public int primaryZoneCount();

	public int getMaxActiveUDPThreadCount();

	public long getUDPQueueSize();

	public long getCompletedUDPQueryCount();

	public int getUDPThreadPoolSize();

	public int getActiveUDPThreadCount();

	public int getMaxActiveTCPThreadCount();

	public long getTCPQueueSize();

	public long getCompletedTCPQueryCount();

	public int getTCPThreadPoolSize();
	
	public Resolver getResolver(String name);
	
	public List<Entry<String,Resolver>> getResolvers();
	
	public ZoneProvider getZoneProvider(String name);
	
	public Set<Entry<String,ZoneProvider>> getZoneProviders();
	
	public Plugin getPlugin(String name);
	
	public Set<Entry<String,Plugin>> getPlugins();
}
