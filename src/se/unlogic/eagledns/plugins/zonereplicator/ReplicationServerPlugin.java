package se.unlogic.eagledns.plugins.zonereplicator;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import se.unlogic.eagledns.plugins.BasePlugin;
import se.unlogic.eagledns.zoneproviders.db.beans.DBZone;
import se.unlogic.standardutils.collections.CollectionUtils;
import se.unlogic.standardutils.dao.AnnotatedDAO;
import se.unlogic.standardutils.dao.HighLevelQuery;
import se.unlogic.standardutils.dao.QueryOperators;
import se.unlogic.standardutils.dao.QueryParameterFactory;
import se.unlogic.standardutils.dao.SimpleAnnotatedDAOFactory;
import se.unlogic.standardutils.dao.SimpleDataSource;
import se.unlogic.standardutils.dao.TransactionHandler;
import se.unlogic.standardutils.numbers.NumberUtils;
import se.unlogic.standardutils.rmi.PasswordLogin;


public class ReplicationServerPlugin extends BasePlugin implements Remote, ReplicationServer{

	private String rmiPassword;
	private Integer rmiPort;
	
	private String driver;
	private String url;
	private String username;
	private String password;

	private AnnotatedDAO<DBZone> zoneDAO;	
	private HighLevelQuery<DBZone> allZonesQuery = new HighLevelQuery<DBZone>(DBZone.RECORDS_RELATION);	
	private QueryParameterFactory<DBZone, Integer> zoneIDParamFactory;
	private QueryParameterFactory<DBZone, Long> serialParamFactory;
	private QueryParameterFactory<DBZone, Boolean> enabledParamFactory;
	
	private ReplicationLoginHandler replicationLoginHandler;

	@Override
	public void init(String name) throws Exception {

		super.init(name);
		
		DataSource dataSource;

		try {
			dataSource = new SimpleDataSource(driver, url, username, password);

		} catch (ClassNotFoundException e) {

			log.error("Unable to load JDBC driver " + driver, e);

			throw e;
		}

		SimpleAnnotatedDAOFactory annotatedDAOFactory = new SimpleAnnotatedDAOFactory();

		this.zoneDAO = new AnnotatedDAO<DBZone>(dataSource,DBZone.class, annotatedDAOFactory);
		this.zoneIDParamFactory = zoneDAO.getParamFactory("zoneID", Integer.class);
		this.serialParamFactory = zoneDAO.getParamFactory("serial", Long.class);
		this.enabledParamFactory = zoneDAO.getParamFactory("enabled", boolean.class);
		
		super.init(name);
		
		if (rmiPassword == null || rmiPort == null) {

			throw new RuntimeException("RMI port and/or password not set");

		} else {

			replicationLoginHandler = new ReplicationLoginHandler(this, rmiPassword);

			try {
				@SuppressWarnings("unchecked")
				PasswordLogin<ReplicationServerPlugin> loginHandler = (PasswordLogin<ReplicationServerPlugin>) UnicastRemoteObject.exportObject(replicationLoginHandler, rmiPort);
				UnicastRemoteObject.exportObject(this, rmiPort);

				Registry registry = LocateRegistry.createRegistry(rmiPort);

				registry.bind("replicationLoginHandler", loginHandler);

			} catch (AccessException e) {

				throw e;

			} catch (RemoteException e) {

				throw e;

			} catch (AlreadyBoundException e) {

				throw e;
			}
		}
		
		log.info("Plugin " + this.name + " started with RMI interface on port " + rmiPort);
	}	
	
	/* (non-Javadoc)
	 * @see se.unlogic.eagledns.plugins.zonereplicator.ReplicationServer#replicate(java.util.List)
	 */
	public ReplicationResponse replicate(List<DBZone> clientZones) throws ReplicationException, RemoteException, ServerNotActiveException{
		
		String clientURL = UnicastRemoteObject.getClientHost();
		
		log.debug("Starting replication for client connecting from " + clientURL + " with " + CollectionUtils.getSize(clientZones) + " zones.");
		
		TransactionHandler transactionHandler = null;
		
		try{
			transactionHandler = zoneDAO.createTransaction();
			
			//Client has no zones
			if(clientZones == null){
				
				List<DBZone> dbZones = zoneDAO.getAll(allZonesQuery, transactionHandler);
				
				//Server has no zones either
				if(dbZones == null){
					
					return null;
				}
				
				//Return found zones
				ReplicationResponse response = new ReplicationResponse(dbZones, null, null);
				
				log.info("Replication changes found for client connecting from " + clientURL + " sending " + response);
				return response;
			}
			
			List<DBZone> newZones = getNewZones(clientZones,transactionHandler);
			List<DBZone> updatedZones = getUpdatedZones(clientZones,transactionHandler);
			List<DBZone> deletedZones = getDeletedZones(clientZones,transactionHandler);
			
			transactionHandler.commit();
			
			//No changes found
			if(newZones == null && updatedZones == null && deletedZones == null){
				
				log.debug("Replication finished, no changes found");
				return null;
			}
			
			//Return detected changes
			ReplicationResponse response = new ReplicationResponse(newZones, updatedZones, deletedZones);
			
			log.info("Replication changes found for client connecting from " + clientURL + " sending " + response);
			
			return response;
			
		} catch (SQLException e) {

			log.error("Error during replication", e);
			throw new ReplicationException();
			
		}catch (RuntimeException e) {

			log.error("Error during replication", e);
			throw new ReplicationException();
			
		}finally{
		
			TransactionHandler.autoClose(transactionHandler);
		}
	}

	private List<DBZone> getNewZones(List<DBZone> clientZones, TransactionHandler transactionHandler) throws SQLException {

		HighLevelQuery<DBZone> query = new HighLevelQuery<DBZone>(DBZone.RECORDS_RELATION);
		
		List<Integer> zoneIDList = new ArrayList<Integer>(clientZones.size());
		
		for(DBZone dbZone : clientZones){
			
			zoneIDList.add(dbZone.getZoneID());
		}
		
		query.addParameter(zoneIDParamFactory.getWhereNotInParameter(zoneIDList));
		
		return zoneDAO.getAll(query, transactionHandler);
	}

	private List<DBZone> getUpdatedZones(List<DBZone> clientZones, TransactionHandler transactionHandler) throws SQLException {

		List<DBZone> updatedZones = new ArrayList<DBZone>(clientZones.size());
		
		for(DBZone dbZone : clientZones){
			
			HighLevelQuery<DBZone> query = new HighLevelQuery<DBZone>(DBZone.RECORDS_RELATION);
			
			query.addParameter(zoneIDParamFactory.getParameter(dbZone.getZoneID()));
			
			if(dbZone.getSerial() != null){
				
				query.addParameter(serialParamFactory.getParameter(dbZone.getSerial(), QueryOperators.NOT_EQUALS));
				
			}else{
				
				query.addParameter(serialParamFactory.getIsNotNullParameter());
			}
			
			DBZone updatedZone = zoneDAO.get(query, transactionHandler);
			
			if(updatedZone != null){
				
				updatedZones.add(updatedZone);
				continue;
			}
			
			query = new HighLevelQuery<DBZone>(DBZone.RECORDS_RELATION);
			
			query.addParameter(zoneIDParamFactory.getParameter(dbZone.getZoneID()));
			query.addParameter(enabledParamFactory.getParameter(dbZone.isEnabled(), QueryOperators.NOT_EQUALS));
			
			updatedZone = zoneDAO.get(query, transactionHandler);
			
			if(updatedZone != null){
				
				updatedZones.add(updatedZone);
			}
		}
		
		if(updatedZones.isEmpty()){
			
			return null;
		}
		
		clientZones.removeAll(updatedZones);
		
		return updatedZones;
	}
	
	private List<DBZone> getDeletedZones(List<DBZone> clientZones, TransactionHandler transactionHandler) throws SQLException {

		List<DBZone> deletedZones = new ArrayList<DBZone>(clientZones.size());
		
		for(DBZone dbZone : clientZones){
			
			if(!zoneDAO.beanExists(dbZone, transactionHandler)){
				
				deletedZones.add(dbZone);
			}
		}
		
		if(deletedZones.isEmpty()){
			
			return null;
		}
		
		return deletedZones;
	}
	
	public void setRmiServerHostname(String serverHost){
		
		System.getProperties().put("java.rmi.server.hostname", serverHost);
	}

	
	public void setRmiPassword(String rmiPassword) {
	
		this.rmiPassword = rmiPassword;
	}

	
	public void setRmiPort(String rmiPort) {
	
		this.rmiPort = NumberUtils.toInt(rmiPort);
	}

	
	public void setDriver(String driver) {
	
		this.driver = driver;
	}

	
	public void setUrl(String url) {
	
		this.url = url;
	}

	
	public void setUsername(String username) {
	
		this.username = username;
	}

	
	public void setPassword(String password) {
	
		this.password = password;
	}	
}
