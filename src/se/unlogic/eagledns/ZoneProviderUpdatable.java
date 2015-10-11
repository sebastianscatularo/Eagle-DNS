/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import se.unlogic.eagledns.zoneproviders.ZoneProvider;


/**
 * Interface that tells Eagle DNS that a {@link ZoneProvider} can trigger a zone reload.
 * 
 * @author Robert "Unlogic" Olofsson (unlogic@unlogic.se)
 * @author Michael Neale, Red Hat (JBoss division)
 * 
 */
public interface ZoneProviderUpdatable {

	/**
	 * This method is automatically called by Eagle DNS when the {@link ZoneProvider} has been instantiated, before the {@link ZoneProvider#init(String) init()} method is called.
	 * 
	 * @see ZoneChangeCallback
	 * 
	 * @param zoneChangeCallback Callback handle
	 */
	void setChangeListener(ZoneChangeCallback zoneChangeCallback);

}
