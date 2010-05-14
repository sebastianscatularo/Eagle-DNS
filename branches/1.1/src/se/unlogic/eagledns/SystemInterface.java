package se.unlogic.eagledns;

import org.xbill.DNS.Name;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.Zone;


/**
 * This interface is used by {@link Resolver}'s to access the internal functions of Eagle DNS
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

}
