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