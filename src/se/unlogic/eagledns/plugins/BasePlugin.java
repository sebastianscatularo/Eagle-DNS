/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins;

import org.apache.log4j.Logger;

import se.unlogic.eagledns.SystemInterface;


public abstract class BasePlugin implements Plugin {

	protected Logger log = Logger.getLogger(this.getClass());
	
	protected SystemInterface systemInterface;
	
	protected String name;
	
	public void setSystemInterface(SystemInterface systemInterface) {

		this.systemInterface = systemInterface;
	}

	public void init(String name) throws Exception{
		
		this.name = name;
	}

	public void shutdown() throws Exception {}
}
