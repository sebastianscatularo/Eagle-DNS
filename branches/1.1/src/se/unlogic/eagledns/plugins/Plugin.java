/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins;

import se.unlogic.eagledns.SystemInterface;


/**
 * A general purpose plugin interface for creating Eagle DNS plugins
 * 
 * @author Robert "Unlogic" Olofsson
 *
 */
public interface Plugin {

	/**
	 * This method is called after the plugin has been instantiated by EagleDNS and all properties
	 * specified in the config file for this plugin have been set using their set methods.
	 */
	public void init(String name) throws Exception;
	
	/**
	 * @param systemInterface
	 *            used to access the internal functions of Eagle DNS
	 */
	public void setSystemInterface(SystemInterface systemInterface);
	
	/**
	 * This method is called when EagleDNS is shutting down and all queued queries have been processed
	 */
	public void shutdown() throws Exception;
}
