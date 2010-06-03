package se.unlogic.eagledns.resolvers;

import org.xbill.DNS.Type;


public class ASpoofingResolver extends SpoofingResolver{

	@Override
	protected int getRecordType() {

		return Type.A;
	}
}
