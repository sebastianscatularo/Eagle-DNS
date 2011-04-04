/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins;

import java.rmi.RemoteException;

import se.unlogic.eagledns.SystemInterface;
import se.unlogic.eagledns.plugins.remotemanagement.EagleManager;

public class SystemInterfaceWrapper implements EagleManager {

	private SystemInterface systemInterface;

	public SystemInterfaceWrapper(SystemInterface systemInterface) {

		super();
		this.systemInterface = systemInterface;
	}

	public int getActiveUDPThreadCount() throws RemoteException {

		return systemInterface.getActiveUDPThreadCount();
	}

	public long getCompletedTCPQueryCount() throws RemoteException {

		return systemInterface.getCompletedTCPQueryCount();
	}

	public long getCompletedUDPQueryCount() throws RemoteException {

		return systemInterface.getCompletedUDPQueryCount();
	}

	public int getMaxActiveTCPThreadCount() throws RemoteException {

		return systemInterface.getMaxActiveTCPThreadCount();
	}

	public int getMaxActiveUDPThreadCount() throws RemoteException {

		return systemInterface.getMaxActiveUDPThreadCount();
	}

	public int getResolverCount() throws RemoteException {

		return systemInterface.getResolverCount();
	}

	public long getStartTime() throws RemoteException {

		return systemInterface.getStartTime();
	}

	public String getVersion() throws RemoteException {

		return systemInterface.getVersion();
	}

	public int primaryZoneCount() throws RemoteException {

		return systemInterface.primaryZoneCount();
	}

	public void reloadZones() throws RemoteException {

		systemInterface.reloadZones();
	}

	public int secondaryZoneCount() throws RemoteException {

		return systemInterface.secondaryZoneCount();
	}

	public void shutdown() throws RemoteException {

		new Thread() {

			@Override
			public void run() {

				//RMI thread workaround
				systemInterface.shutdown();

			}
		}.start();
	}

	public int getActiveTCPThreadCount() throws RemoteException {

		return systemInterface.getActiveUDPThreadCount();
	}

	public int getUDPThreadPoolMaxSize() throws RemoteException {

		return systemInterface.getUDPThreadPoolMaxSize();
	}

	public int getUDPThreadPoolMinSize() throws RemoteException {

		return systemInterface.getUDPThreadPoolMinSize();
	}

	public int getTCPThreadPoolMaxSize() throws RemoteException {

		return systemInterface.getTCPThreadPoolMaxSize();
	}

	public int getTCPThreadPoolMinSize() throws RemoteException {

		return systemInterface.getTCPThreadPoolMinSize();
	}

	public int getZoneProviderCount() throws RemoteException {

		return systemInterface.getZoneProviders().size();
	}

	public int getPluginCount() throws RemoteException {

		return systemInterface.getPlugins().size();
	}

	public long getRejectedUDPConnections() {

		return systemInterface.getRejectedUDPConnections();
	}

	public long getRejectedTCPConnections() {

		return systemInterface.getRejectedTCPConnections();
	}
}
