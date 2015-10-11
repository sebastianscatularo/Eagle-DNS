/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins.remotemanagement;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EagleManager extends Remote {

	public void shutdown() throws RemoteException;

	public void reloadZones() throws RemoteException;

	public String getVersion() throws RemoteException;

	public long getStartTime() throws RemoteException;

	public int getZoneProviderCount() throws RemoteException;
	
	public int getPluginCount() throws RemoteException;
	
	public int getResolverCount() throws RemoteException;

	public int secondaryZoneCount() throws RemoteException;

	public int primaryZoneCount() throws RemoteException;

	public int getMaxActiveUDPThreadCount() throws RemoteException;

	public long getCompletedUDPQueryCount() throws RemoteException;

	public int getActiveUDPThreadCount() throws RemoteException;

	public int getMaxActiveTCPThreadCount() throws RemoteException;

	public long getCompletedTCPQueryCount() throws RemoteException;

	public int getActiveTCPThreadCount() throws RemoteException;

	public int getUDPThreadPoolMaxSize() throws RemoteException;

	public int getUDPThreadPoolMinSize() throws RemoteException;

	public int getTCPThreadPoolMaxSize() throws RemoteException;

	public int getTCPThreadPoolMinSize() throws RemoteException;
	
	public long getRejectedUDPConnections() throws RemoteException;

	public long getRejectedTCPConnections() throws RemoteException;	
}
