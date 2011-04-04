/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins.remotemanagement;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import se.unlogic.standardutils.settings.XMLSettingNode;
import se.unlogic.standardutils.time.TimeUtils;


public class EagleManagerClient {

	public static EagleManager getManager(String host, int port, String password) throws RemoteException, NotBoundException {

		Registry registry = LocateRegistry.getRegistry(host,port);

		EagleLogin eagleLogin = (EagleLogin) registry.lookup("eagleLogin");

		return eagleLogin.login(password);
	}



	public static void main(String[] args) {

		if(args.length != 3 || (!args[2].equals("reload") && !args[2].equals("shutdown") && !args[2].equals("info"))){

			System.out.println("Usage EagleManagerClient config host command");
			System.out.println("Valid commands are: reload, shutdown, info");
			return;
		}

		XMLSettingNode configFile;

		try {
			configFile = new XMLSettingNode(args[0]);

		} catch (Exception e) {

			System.out.println("Unable to open config file " + args[0] + "!");
			return;
		}

		XMLSettingNode rmiRemoteManagementPluginElement = configFile.getSetting("/Config/Plugins/Plugin[Class='" + RMIRemoteManagementPlugin.class.getName() + "']");
		
		if(rmiRemoteManagementPluginElement == null){
			
			System.out.println("No RMI remote management plugin found in config!");
			return;
		}
		
		String password = rmiRemoteManagementPluginElement.getString("Properties/Property[@name='password']");

		if(password == null){

			System.out.println("No remote management password found in config!");
			return;
		}

		Integer port = rmiRemoteManagementPluginElement.getInt("Properties/Property[@name='port']");

		if(port == null){

			System.out.println("No remote management port found in config!");
			return;
		}

		try {
			EagleManager eagleManager = getManager(args[1], port, password);

			if(eagleManager == null){

				System.out.println("Invalid password!");

			}else{

				if(args[2].equals("reload")){

					eagleManager.reloadZones();
					System.out.println("Zones reloaded");

				}else if(args[2].equals("info")){

					System.out.println("Getting information...");

					System.out.println("Version: " + eagleManager.getVersion());
					System.out.println("Uptime: " + TimeUtils.millisecondsToString(System.currentTimeMillis() - eagleManager.getStartTime()));
					System.out.println();
					System.out.println("Plugins: " + eagleManager.getPluginCount());
					System.out.println("Zone provider: " + eagleManager.getZoneProviderCount());
					System.out.println("Resolvers: " + eagleManager.getResolverCount());
					System.out.println("Primary zones: " + eagleManager.primaryZoneCount());
					System.out.println("Secondary zones: " + eagleManager.secondaryZoneCount());
					System.out.println();
					System.out.println("TCP Thread Pool");
					System.out.println("\tMin size: " + eagleManager.getTCPThreadPoolMinSize());
					System.out.println("\tMax size: " + eagleManager.getTCPThreadPoolMaxSize());
					System.out.println("\tActive threads: " + eagleManager.getActiveTCPThreadCount());
					System.out.println("\tMax active threads: " + eagleManager.getMaxActiveTCPThreadCount());
					System.out.println("\tCompleted query count: " + eagleManager.getCompletedTCPQueryCount());
					System.out.println("\tRejected connection count: " + eagleManager.getRejectedTCPConnections());
					System.out.println();
					System.out.println("UDP Thread Pool");
					System.out.println("\tMin size: " + eagleManager.getUDPThreadPoolMinSize());
					System.out.println("\tMax size: " + eagleManager.getUDPThreadPoolMaxSize());					
					System.out.println("\tActive threads: " + eagleManager.getActiveUDPThreadCount());
					System.out.println("\tMax active threads: " + eagleManager.getMaxActiveUDPThreadCount());
					System.out.println("\tCompleted query count: " + eagleManager.getCompletedUDPQueryCount());
					System.out.println("\tRejected connection count: " + eagleManager.getRejectedUDPConnections());


				}else{

					eagleManager.shutdown();
					System.out.println("Shutdown command sent");
				}
			}

		} catch (RemoteException e) {

			System.out.println("Unable to connect " + e);

		} catch (NotBoundException e) {

			System.out.println("Unable to connect " + e);
		}
	}
}
