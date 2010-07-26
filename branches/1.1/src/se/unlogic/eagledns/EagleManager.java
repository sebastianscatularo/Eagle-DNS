/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EagleManager extends Remote{

	public void shutdown() throws RemoteException;

	public void reloadZones() throws RemoteException;

	public String getVersion() throws RemoteException;

	public long getStartTime() throws RemoteException;

	public int getResolverCount() throws RemoteException;

	public int secondaryZoneCount() throws RemoteException;

	public int primaryZoneCount() throws RemoteException;

	public int getMaxActiveUDPThreadCount() throws RemoteException;

	public long getUDPQueueSize() throws RemoteException;

	public long getCompletedUDPQueryCount() throws RemoteException;

	public int getUDPThreadPoolSize() throws RemoteException;

	public int getActiveUDPThreadCount() throws RemoteException;

	public int getMaxActiveTCPThreadCount() throws RemoteException;

	public long getTCPQueueSize() throws RemoteException;

	public long getCompletedTCPQueryCount() throws RemoteException;

	public int getTCPThreadPoolSize() throws RemoteException;

	public int getActiveTCPThreadCount() throws RemoteException;
}
