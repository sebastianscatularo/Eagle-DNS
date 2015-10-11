/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins.remotemanagement;

import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;



public class LoginHandler implements EagleLogin {

	private Logger log = Logger.getLogger(this.getClass());

	private EagleManager eagleManager;
	private String password;

	public LoginHandler(EagleManager eagleManager, String password) {
		super();
		this.eagleManager = eagleManager;
		this.password = password;
	}

	public EagleManager login(String password) {

		if(password != null && password.equalsIgnoreCase(this.password)){

			try {
				log.info("Remote login from " + UnicastRemoteObject.getClientHost());
			} catch (ServerNotActiveException e) {}

			return eagleManager;

		}

		try {
			log.warn("Failed login attempt from " + UnicastRemoteObject.getClientHost());
		} catch (ServerNotActiveException e) {}

		return null;
	}
}
