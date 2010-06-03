package se.unlogic.eagledns.plugins;

import java.rmi.RemoteException;

import se.unlogic.eagledns.EagleManager;
import se.unlogic.eagledns.SystemInterface;


public class SystemInterfaceWrapper implements EagleManager{

	private SystemInterface systemInterface;

	public SystemInterfaceWrapper(SystemInterface systemInterface){

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

	public long getTCPQueueSize() throws RemoteException {

		return systemInterface.getTCPQueueSize();
	}

	public int getTCPThreadPoolSize() throws RemoteException {

		return systemInterface.getTCPThreadPoolSize();
	}

	public long getUDPQueueSize() throws RemoteException {

		return systemInterface.getUDPQueueSize();
	}

	public int getUDPThreadPoolSize() throws RemoteException {

		return systemInterface.getUDPThreadPoolSize();
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

		systemInterface.shutdown();
	}

	public int getActiveTCPThreadCount() throws RemoteException {

		return systemInterface.getActiveUDPThreadCount();
	}
}
