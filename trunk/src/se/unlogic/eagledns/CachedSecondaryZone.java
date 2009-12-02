package se.unlogic.eagledns;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;
import org.xbill.DNS.Zone;
import org.xbill.DNS.ZoneTransferException;
import org.xbill.DNS.ZoneTransferIn;


public class CachedSecondaryZone extends CachedPrimaryZone {

	private Logger log = Logger.getLogger(this.getClass());

	private SecondaryZone secondaryZone;
	private Long lastChecked;

	public CachedSecondaryZone(ZoneProvider zoneProvider, SecondaryZone secondaryZone) {

		super(null, zoneProvider);
		this.secondaryZone = secondaryZone;
		//this.update();

		if(this.zone == null && this.secondaryZone.getZoneBackup() != null){

			log.info("Using backup zone data for sedondary zone " + this.secondaryZone.getZoneName());

			this.zone = this.secondaryZone.getZoneBackup();
		}
	}


	public SecondaryZone getSecondaryZone() {

		return secondaryZone;
	}


	public void setSecondaryZone(SecondaryZone secondaryZone) {

		this.secondaryZone = secondaryZone;
	}

	public Long getLastChecked() {

		return lastChecked;
	}

	public void setLastChecked(Long lastChecked) {

		this.lastChecked = lastChecked;
	}


	/**
	 * Updates this secondary zone from the primary zone
	 * @param axfrTimeout
	 */
	public void update(int axfrTimeout) {


		try {
			ZoneTransferIn xfrin = ZoneTransferIn.newAXFR(this.secondaryZone.getZoneName(), this.secondaryZone.getRemoteServerAddress(), null);
			xfrin.setDClass(DClass.value(this.secondaryZone.getDclass()));
			xfrin.setTimeout(axfrTimeout);

			List<?> records = xfrin.run();

			if (!xfrin.isAXFR()) {

				log.warn("Unable to transfer zone " + this.secondaryZone.getZoneName() + " from server " + this.secondaryZone.getRemoteServerAddress() + ", response is not a valid AXFR!");

				return;
			}

			this.zone = new Zone(this.secondaryZone.getZoneName(),records.toArray(new Record[records.size()]));

			this.secondaryZone.setZoneBackup(zone);

			this.zoneProvider.zoneUpdated(this.secondaryZone);

			log.info("Zone " + this.secondaryZone.getZoneName() + " successfully transfered from server " + this.secondaryZone.getRemoteServerAddress());

		} catch (IOException e) {

			log.warn("Unable to transfer zone " + this.secondaryZone.getZoneName() + " from server " + this.secondaryZone.getRemoteServerAddress() + ", " + e);

		} catch (ZoneTransferException e) {

			log.warn("Unable to transfer zone " + this.secondaryZone.getZoneName() + " from server " + this.secondaryZone.getRemoteServerAddress() + ", " + e);

		}finally{

			this.lastChecked = System.currentTimeMillis();
		}
	}
}
