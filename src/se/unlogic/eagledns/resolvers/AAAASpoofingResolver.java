package se.unlogic.eagledns.resolvers;

import org.xbill.DNS.Type;


public class AAAASpoofingResolver extends SpoofingResolver{

	@Override
	protected int getRecordType() {

		return Type.AAAA;
	}
}
