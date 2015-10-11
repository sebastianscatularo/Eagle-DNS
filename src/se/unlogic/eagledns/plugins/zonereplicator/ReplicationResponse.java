package se.unlogic.eagledns.plugins.zonereplicator;

import java.io.Serializable;
import java.util.List;

import se.unlogic.eagledns.zoneproviders.db.beans.DBZone;
import se.unlogic.standardutils.collections.CollectionUtils;

public class ReplicationResponse implements Serializable{

	private static final long serialVersionUID = 4935055334918731111L;

	protected List<DBZone> newZones;
	protected List<DBZone> updatedZones;
	protected List<DBZone> deletedZones;

	public ReplicationResponse(List<DBZone> newZones, List<DBZone> updatedZones, List<DBZone> deletedZones) {

		super();
		this.newZones = newZones;
		this.updatedZones = updatedZones;
		this.deletedZones = deletedZones;
	}

	public List<DBZone> getNewZones() {

		return newZones;
	}

	public List<DBZone> getUpdatedZones() {

		return updatedZones;
	}

	public List<DBZone> getDeletedZones() {

		return deletedZones;
	}

	@Override
	public String toString() {
		
		return CollectionUtils.getSize(newZones) + " new zones, " + CollectionUtils.getSize(updatedZones) + " updated zones, " + CollectionUtils.getSize(deletedZones) + " deleted zones";
	}
}
