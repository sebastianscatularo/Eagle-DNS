/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.zoneproviders.db;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.xbill.DNS.Zone;

import se.unlogic.eagledns.SecondaryZone;
import se.unlogic.eagledns.SystemInterface;
import se.unlogic.eagledns.zoneproviders.ZoneProvider;
import se.unlogic.eagledns.zoneproviders.db.beans.DBRecord;
import se.unlogic.eagledns.zoneproviders.db.beans.DBSecondaryZone;
import se.unlogic.eagledns.zoneproviders.db.beans.DBZone;
import se.unlogic.standardutils.dao.AnnotatedDAO;
import se.unlogic.standardutils.dao.HighLevelQuery;
import se.unlogic.standardutils.dao.QueryParameterFactory;
import se.unlogic.standardutils.dao.SimpleAnnotatedDAOFactory;
import se.unlogic.standardutils.dao.SimpleDataSource;
import se.unlogic.standardutils.dao.TransactionHandler;

public class DBZoneProvider implements ZoneProvider {

	private Logger log = Logger.getLogger(this.getClass());

	private String name;
	private String driver;
	private String url;
	private String username;
	private String password;

	private AnnotatedDAO<DBZone> zoneDAO;
	private AnnotatedDAO<DBRecord> recordDAO;
	private HighLevelQuery<DBZone> primaryZoneQuery;
	private HighLevelQuery<DBZone> secondaryZoneQuery;
	private QueryParameterFactory<DBZone, Integer> zoneIDQueryParameterFactory;
	private QueryParameterFactory<DBRecord, DBZone> recordZoneQueryParameterFactory;

	public void init(String name) throws ClassNotFoundException {

		this.name = name;

		DataSource dataSource;

		try {
			dataSource = new SimpleDataSource(driver, url, username, password);

		} catch (ClassNotFoundException e) {

			log.error("Unable to load JDBC driver " + driver, e);

			throw e;
		}

		SimpleAnnotatedDAOFactory annotatedDAOFactory = new SimpleAnnotatedDAOFactory();

		this.zoneDAO = new AnnotatedDAO<DBZone>(dataSource,DBZone.class, annotatedDAOFactory);
		this.recordDAO = new AnnotatedDAO<DBRecord>(dataSource,DBRecord.class, annotatedDAOFactory);

		QueryParameterFactory<DBZone, Boolean> zoneTypeParamFactory = zoneDAO.getParamFactory("secondary", boolean.class);
		QueryParameterFactory<DBZone, Boolean> enabledParamFactory = zoneDAO.getParamFactory("enabled", boolean.class);
		
		this.primaryZoneQuery = new HighLevelQuery<DBZone>();
		this.primaryZoneQuery.addParameter(zoneTypeParamFactory.getParameter(false));
		this.primaryZoneQuery.addParameter(enabledParamFactory.getParameter(true));
		this.primaryZoneQuery.addRelation(DBZone.RECORDS_RELATION);
		
		this.secondaryZoneQuery = new HighLevelQuery<DBZone>();
		this.secondaryZoneQuery.addParameter(zoneTypeParamFactory.getParameter(true));
		this.secondaryZoneQuery.addParameter(enabledParamFactory.getParameter(true));
		this.secondaryZoneQuery.addRelation(DBZone.RECORDS_RELATION);
		
		this.zoneIDQueryParameterFactory = zoneDAO.getParamFactory("zoneID", Integer.class);
		this.recordZoneQueryParameterFactory = recordDAO.getParamFactory("zone", DBZone.class);
	}

	public Collection<Zone> getPrimaryZones() {

		try {
			List<DBZone> dbZones = this.zoneDAO.getAll(primaryZoneQuery);

			if(dbZones != null){

				ArrayList<Zone> zones = new ArrayList<Zone>(dbZones.size());

				for(DBZone dbZone : dbZones){

					try {
						zones.addAll(dbZone.toZones());

					} catch (IOException e) {

						log.error("Unable to parse zone " + dbZone.getName(),e);
					}
				}

				return zones;
			}

		} catch (SQLException e) {

			log.error("Error getting primary zones from DB zone provider " + name,e);
		}

		return null;
	}

	public Collection<SecondaryZone> getSecondaryZones() {

		try {
			List<DBZone> dbZones = this.zoneDAO.getAll(this.secondaryZoneQuery);

			if(dbZones != null){

				ArrayList<SecondaryZone> zones = new ArrayList<SecondaryZone>(dbZones.size());

				for(DBZone dbZone : dbZones){

					try {
						DBSecondaryZone secondaryZone = new DBSecondaryZone(dbZone.getZoneID() ,dbZone.getName(), dbZone.getPrimaryDNS(), dbZone.getDclass());

						if(dbZone.getRecords() != null){
							secondaryZone.setZoneCopy(dbZone.toZone());
							secondaryZone.setDownloaded(dbZone.getDownloaded());
						}

						zones.add(secondaryZone);

					} catch (IOException e) {

						log.error("Unable to parse zone " + dbZone.getName(),e);
					}
				}

				return zones;
			}

		} catch (SQLException e) {

			log.error("Error getting secondary zones from DB zone provider " + name,e);
		}

		return null;
	}

	public void zoneUpdated(SecondaryZone zone) {

		if(!(zone instanceof DBSecondaryZone)){

			log.warn(zone.getClass() + " is not an instance of " + DBSecondaryZone.class + ", ignoring zone update");

			return;
		}

		Integer zoneID = ((DBSecondaryZone)zone).getZoneID();

		TransactionHandler transactionHandler = null;

		try {
			transactionHandler = zoneDAO.createTransaction();

			DBZone dbZone = this.zoneDAO.get(new HighLevelQuery<DBZone>(this.zoneIDQueryParameterFactory.getParameter(zoneID),(Field)null),transactionHandler);

			if(dbZone == null){

				log.warn("Unable to find secondary zone with zoneID " + zoneID + " in DB, ignoring zone update");

				return;
				
			}else if(!dbZone.isEnabled()){
				
				log.warn("Secondary zone with zone " + dbZone + " is disabled in DB ignoring AXFR update");

				return;
			}

			dbZone.parse(zone.getZoneCopy(), true);

			zoneDAO.update(dbZone,transactionHandler, null);

			recordDAO.delete(new HighLevelQuery<DBRecord>(recordZoneQueryParameterFactory.getParameter(dbZone),(Field)null), transactionHandler);

			if(dbZone.getRecords() != null){

				for(DBRecord dbRecord : dbZone.getRecords()){

					dbRecord.setZone(dbZone);

					this.recordDAO.add(dbRecord, transactionHandler, null);
				}
			}

			transactionHandler.commit();

			log.debug("Changes in seconday zone " + dbZone + " saved");

		} catch (SQLException e) {

			log.error("Unable to save changes in secondary zone " + zone.getZoneName(), e);
			TransactionHandler.autoClose(transactionHandler);
		}
	}

	public void zoneChecked(SecondaryZone zone) {

		if(!(zone instanceof DBSecondaryZone)){

			log.warn(zone.getClass() + " is not an instance of " + DBSecondaryZone.class + ", ignoring zone check");

			return;
		}

		Integer zoneID = ((DBSecondaryZone)zone).getZoneID();

		TransactionHandler transactionHandler = null;

		try {
			transactionHandler = zoneDAO.createTransaction();

			DBZone dbZone = this.zoneDAO.get(new HighLevelQuery<DBZone>(this.zoneIDQueryParameterFactory.getParameter(zoneID), (Field)null),transactionHandler);

			if(dbZone == null){

				log.warn("Unable to find secondary zone with zoneID " + zoneID + " in DB, ignoring zone check");

				return;
				
			}else if(!dbZone.isEnabled()){
				
				log.warn("Secondary zone with zone " + dbZone + " is disabled in DB ignoring zone check");

				return;
			}

			dbZone.setDownloaded(new Timestamp(System.currentTimeMillis()));

			zoneDAO.update(dbZone,transactionHandler, null);

			transactionHandler.commit();

			log.debug("Download timestamp of seconday zone " + dbZone + " updated");

		} catch (SQLException e) {

			log.error("Unable to update download of secondary zone " + zone.getZoneName(), e);
			TransactionHandler.autoClose(transactionHandler);
		}
	}

	public void shutdown() {

		//Nothing to do here...
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

	public void setSystemInterface(SystemInterface systemInterface) {}
}
