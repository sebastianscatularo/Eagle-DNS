/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins.zonereplicator;

import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import se.unlogic.standardutils.rmi.PasswordLogin;


public class ReplicationLoginHandler implements PasswordLogin<ReplicationServerPlugin>{

	private Logger log = Logger.getLogger(this.getClass());

	private ReplicationServerPlugin server;
	private String password;

	public ReplicationLoginHandler(ReplicationServerPlugin server, String password) {
		super();
		this.server = server;
		this.password = password;
	}

	public ReplicationServerPlugin login(String password) throws RemoteException{

		if(password != null && password.equals(this.password)){

			try {
				log.debug("Remote login from " + UnicastRemoteObject.getClientHost());
			} catch (ServerNotActiveException e) {}

			return server;

		}

		try {
			log.warn("Failed login attempt from " + UnicastRemoteObject.getClientHost());
		} catch (ServerNotActiveException e) {}

		return null;
	}
}
