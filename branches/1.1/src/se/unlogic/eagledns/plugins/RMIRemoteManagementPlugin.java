/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import se.unlogic.eagledns.EagleLogin;
import se.unlogic.eagledns.EagleManager;
import se.unlogic.eagledns.LoginHandler;
import se.unlogic.standardutils.numbers.NumberUtils;


public class RMIRemoteManagementPlugin extends BasePlugin{

	private String password;
	private Integer port;
	private LoginHandler loginHandler;
	
	@Override
	public void init(String name) throws Exception {

		super.init(name);
		
		if (password == null || port == null) {

			throw new RuntimeException("Remote managed port and/or password not set, unable to start RMI remote managent plugin.");

		} else {

			log.info("Plugin " + this.name + " starting RMI remote management interface on port " + port);

			EagleManager eagleManager = new SystemInterfaceWrapper(systemInterface);
			
			this.loginHandler = new LoginHandler(eagleManager, this.password);

			try {
				EagleLogin eagleLogin = (EagleLogin) UnicastRemoteObject.exportObject(loginHandler, port);
				UnicastRemoteObject.exportObject(eagleManager, port);

				Registry registry = LocateRegistry.createRegistry(port);

				registry.bind("eagleLogin", eagleLogin);

			} catch (AccessException e) {

				throw e;

			} catch (RemoteException e) {

				throw e;

			} catch (AlreadyBoundException e) {

				throw e;
			}
		}		
	}
	
	public void setPassword(String remotePassword) {
	
		this.password = remotePassword;
	}

	
	public void setPort(String remotePort) {
	
		this.port = NumberUtils.toInt(remotePort);
	}
	
	public void setRMIServerHostname(String serverHost){
		
		System.getProperties().put("java.rmi.server.hostname", serverHost);
	}
}
