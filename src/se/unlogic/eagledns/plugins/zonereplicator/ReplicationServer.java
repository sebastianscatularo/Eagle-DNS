package se.unlogic.eagledns.plugins.zonereplicator;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.List;

import se.unlogic.eagledns.zoneproviders.db.beans.DBZone;

public interface ReplicationServer extends Remote{

	public ReplicationResponse replicate(List<DBZone> clientZones) throws ReplicationException, RemoteException, ServerNotActiveException;

}