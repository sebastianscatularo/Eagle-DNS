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
			this.lastChecked = secondaryZone.getDownloaded().getTime();
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

			Zone axfrZone = new Zone(this.secondaryZone.getZoneName(),records.toArray(new Record[records.size()]));

			log.debug("Zone " + this.secondaryZone.getZoneName() + " successfully transfered from server " + this.secondaryZone.getRemoteServerAddress());

			if(!axfrZone.getSOA().getName().equals(this.secondaryZone.getZoneName())){

				log.warn("Invalid AXFR zone name in response when updating secondary zone " + this.secondaryZone.getZoneName() + ". Got zone name " + axfrZone.getSOA().getName() + " in respons.");
			}

			if(this.zone == null || this.zone.getSOA().getSerial() != axfrZone.getSOA().getSerial()){

				this.zone = axfrZone;

				this.secondaryZone.setZoneBackup(zone);

				this.zoneProvider.zoneUpdated(this.secondaryZone);

				log.info("Zone " + this.secondaryZone.getZoneName() + " successfully updated from server " + this.secondaryZone.getRemoteServerAddress());
			}else{

				log.info("Zone " + this.secondaryZone.getZoneName() + " is already up to date with serial " + axfrZone.getSOA().getSerial());
			}

		} catch (IOException e) {

			checkExpired();

			log.warn("Unable to transfer zone " + this.secondaryZone.getZoneName() + " from server " + this.secondaryZone.getRemoteServerAddress() + ", " + e);

		} catch (ZoneTransferException e) {

			checkExpired();

			log.warn("Unable to transfer zone " + this.secondaryZone.getZoneName() + " from server " + this.secondaryZone.getRemoteServerAddress() + ", " + e);

		}finally{

			this.lastChecked = System.currentTimeMillis();
		}
	}


	private void checkExpired() {

		if(this.secondaryZone.getZoneBackup() != null && (System.currentTimeMillis() - this.secondaryZone.getDownloaded().getTime()) > (this.secondaryZone.getZoneBackup().getSOA().getExpire() * 1000)){

			log.warn("AXFR copy of secondary zone " + secondaryZone.getZoneName() + " has expired, deleting zone data...");
			this.secondaryZone.setZoneBackup(null);

			this.zoneProvider.zoneUpdated(this.secondaryZone);
		}
	}
}
