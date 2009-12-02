package se.unlogic.eagledns.test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import javax.sql.DataSource;

import org.xbill.DNS.Zone;

import se.unlogic.eagledns.zoneproviders.db.beans.DBRecord;
import se.unlogic.eagledns.zoneproviders.db.beans.DBZone;
import se.unlogic.eagledns.zoneproviders.file.FileZoneProvider;
import se.unlogic.utils.dao.AnnotatedDAO;
import se.unlogic.utils.dao.SimpleAnnotatedDAOFactory;
import se.unlogic.utils.dao.SimpleDataSource;
import se.unlogic.utils.dao.TransactionHandler;


public class PrimaryZones2DB {

	public static void main(String[] args) throws ClassNotFoundException, SQLException{

		if(args.length != 5){

			System.out.println("Usage: PrimaryZones2DB zonedir driver url username password");

		}else{

			importZones(args[0], args[1], args[2], args[3], args[4]);
		}
	}

	public static void importZones(String directory, String driver, String url, String username, String password) throws ClassNotFoundException, SQLException {

		FileZoneProvider fileZoneProvider = new FileZoneProvider();

		fileZoneProvider.setZoneFileDirectory(directory);

		Collection<Zone> zones = fileZoneProvider.getPrimaryZones();

		ArrayList<DBZone> dbZones = new ArrayList<DBZone>();

		for(Zone zone : zones){

			System.out.println("Converting zone " + zone.getSOA().getName().toString() + "...");

			dbZones.add(new DBZone(zone,false));
		}

		DataSource dataSource = new SimpleDataSource(driver, url, username, password);

		SimpleAnnotatedDAOFactory annotatedDAOFactory = new SimpleAnnotatedDAOFactory();
		AnnotatedDAO<DBZone> zoneDAO  = new AnnotatedDAO<DBZone>(dataSource,DBZone.class, annotatedDAOFactory);
		AnnotatedDAO<DBRecord> recordDAO  = new AnnotatedDAO<DBRecord>(dataSource,DBRecord.class, annotatedDAOFactory);

		TransactionHandler transactionHandler = zoneDAO.getTransaction();

		try{

			for(DBZone zone : dbZones){

				System.out.println("Storing zone " + zone + "...");

				zoneDAO.add(zone, transactionHandler);

				for(DBRecord dbRecord : zone.getRecords()){

					System.out.println("Storing record " + dbRecord + "...");

					dbRecord.setZone(zone);

					recordDAO.add(dbRecord);
				}
			}

		}catch(Throwable e){

			transactionHandler.abort();
		}
	}
}
