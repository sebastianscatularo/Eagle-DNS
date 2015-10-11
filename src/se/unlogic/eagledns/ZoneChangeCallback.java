/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import se.unlogic.eagledns.plugins.remotemanagement.EagleManager;
import se.unlogic.eagledns.zoneproviders.ZoneProvider;

/**
 * Interface that enables {@link ZoneProvider}'s to reload the zone cache in Eagle DNS without using the remote management interface ({@link EagleManager}).
 * 
 * @author Robert "Unlogic" Olofsson (unlogic@unlogic.se)
 * @author Michael Neale, Red Hat (JBoss division)
 * 
 */
public interface ZoneChangeCallback {

	/**
	 * Calling this method causes Eagle DNS to reload all it's zone from the registered zone providers
	 */
	void zoneDataChanged();

}
