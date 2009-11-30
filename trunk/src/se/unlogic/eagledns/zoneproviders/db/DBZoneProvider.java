package se.unlogic.eagledns.zoneproviders.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.xbill.DNS.Zone;

import se.unlogic.eagledns.SecondaryZone;
import se.unlogic.eagledns.ZoneProvider;
import se.unlogic.eagledns.zoneproviders.db.beans.DBRecord;
import se.unlogic.eagledns.zoneproviders.db.beans.DBZone;
import se.unlogic.utils.dao.AnnotatedDAO;
import se.unlogic.utils.dao.SimpleAnnotatedDAOFactory;

public class DBZoneProvider implements ZoneProvider {

	private Logger log = Logger.getLogger(this.getClass());

	private String driver;
	private String url;
	private String username;
	private String password;

	private AnnotatedDAO<DBZone> zoneDAO;
	
	public void init(String name) throws ClassNotFoundException {

		try {
			Class.forName(driver);

		} catch (ClassNotFoundException e) {

			log.error("Unable to load JDBC driver " + driver, e);
			
			throw e;
		}
		
		this.zoneDAO = new AnnotatedDAO<DBZone>(DBZone.class, new SimpleAnnotatedDAOFactory());
	}

	public Collection<Zone> getPrimaryZones() {

		// TODO Auto-generated method stub
		return null;
	}

	public Collection<SecondaryZone> getSecondayZones() {

		// TODO Auto-generated method stub
		return null;
	}

	public void zoneUpdated(SecondaryZone zone) {

		// TODO Auto-generated method stub

	}

	public void unload() {

		// TODO Auto-generated method stub

	}

	private Connection getConnection() throws SQLException {

		return DriverManager.getConnection(this.url, username, password);
	}

	public void setDriver(String driver) {

		this.driver = driver;
	}

	public void setUsername(String username) {

		this.username = username;
	}

	public void setPassword(String password) {

		this.password = password;
	}

	public void setUrl(String url) {

		this.url = url;
	}
}
