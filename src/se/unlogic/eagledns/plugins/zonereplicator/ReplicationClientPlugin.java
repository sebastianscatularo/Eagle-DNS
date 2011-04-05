package se.unlogic.eagledns.plugins.zonereplicator;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Timer;

import javax.sql.DataSource;

import se.unlogic.eagledns.Status;
import se.unlogic.eagledns.plugins.BasePlugin;
import se.unlogic.eagledns.zoneproviders.db.beans.DBZone;
import se.unlogic.standardutils.dao.AnnotatedDAO;
import se.unlogic.standardutils.dao.HighLevelQuery;
import se.unlogic.standardutils.dao.RelationQuery;
import se.unlogic.standardutils.dao.SimpleAnnotatedDAOFactory;
import se.unlogic.standardutils.dao.SimpleDataSource;
import se.unlogic.standardutils.dao.TransactionHandler;
import se.unlogic.standardutils.numbers.NumberUtils;
import se.unlogic.standardutils.rmi.PasswordLogin;
import se.unlogic.standardutils.time.MillisecondTimeUnits;
import se.unlogic.standardutils.timer.RunnableTimerTask;


public class ReplicationClientPlugin extends BasePlugin implements Runnable{

	private static final HighLevelQuery<DBZone> ALL_ZONES_QUERY;
	
	static{
		ALL_ZONES_QUERY = new HighLevelQuery<DBZone>();
		ALL_ZONES_QUERY.disableAutoRelations(true);
	}
	
	private static final RelationQuery RELATION_QUERY = new RelationQuery(DBZone.RECORDS_RELATION);
	
	private String serverAddress;
	private String rmiPassword;
	private Integer rmiPort;
	
	private String driver;
	private String url;
	private String username;
	private String password;

	private AnnotatedDAO<DBZone> zoneDAO;	
	
	private Timer timer;
	private int replicationInterval = 60;
	
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
		
		this.timer = new Timer(name, true);
		
		timer.scheduleAtFixedRate(new RunnableTimerTask(this), 0, this.replicationInterval * MillisecondTimeUnits.SECOND);
		
		log.info("Plugin " + this.name + " started with replication interval of " + replicationInterval + " seconds.");
	}
	
	@Override
	public void shutdown() throws Exception {

		if(timer != null){
			
			timer.cancel();
		}
		
		super.shutdown();
	}

	public void run() {

		if(systemInterface.getStatus() != Status.STARTED){
			
			log.debug("Incorrect system status skipping replication");
		}
		
		log.debug("Replication starting...");
		
		TransactionHandler transactionHandler = null;
		
		try{
			transactionHandler = zoneDAO.createTransaction();
			
			List<DBZone> zones = zoneDAO.getAll(ALL_ZONES_QUERY, transactionHandler);
			
			ReplicationServer server = this.getServer();
			
			ReplicationResponse response = server.replicate(zones);
			
			//No changes found
			if(response == null){
				
				log.debug("Replication completed succesfully, no changes found on server.");
				return;
			}
			
			log.info("Replication got " + response + " from server " + serverAddress + ":" + rmiPort + ", persisting changes...");
			
			if(response.getNewZones() != null){
				
				zoneDAO.addAll(response.getNewZones(), transactionHandler, RELATION_QUERY);
			}
			
			if(response.getUpdatedZones() != null){
				
				zoneDAO.update(response.getUpdatedZones(), transactionHandler, RELATION_QUERY);
			}
			
			if(response.getDeletedZones() != null){
				
				zoneDAO.delete(response.getDeletedZones(),transactionHandler);
			}
			
			transactionHandler.commit();
			
			log.info("Replication completed succesfully, reloading zones.");
			
			systemInterface.reloadZones();
			
		} catch (ConnectException e) {
			log.warn("Error connecting to server, " + e);
		} catch (Exception e) {
			log.error("Error replicating zones from server", e);
		}finally{
			TransactionHandler.autoClose(transactionHandler);
		}
	}

	@SuppressWarnings("unchecked")
	private ReplicationServer getServer() throws RemoteException, NotBoundException {

		Registry registry = LocateRegistry.getRegistry(serverAddress,rmiPort);

		PasswordLogin<ReplicationServerPlugin> loginHandler = (PasswordLogin<ReplicationServerPlugin>) registry.lookup("replicationLoginHandler");

		return loginHandler.login(rmiPassword);
	}

	
	public void setServerAddress(String serverAddress) {
	
		this.serverAddress = serverAddress;
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
	
	public void setReplicationInterval(String replicationInterval) {
	
		this.replicationInterval = NumberUtils.toInt(replicationInterval);
	}
}
