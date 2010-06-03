package se.unlogic.eagledns.resolvers;

import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TSIGRecord;
import org.xbill.DNS.Type;

import se.unlogic.eagledns.EagleDNS;
import se.unlogic.eagledns.Request;
import se.unlogic.eagledns.plugins.BasePlugin;
import se.unlogic.standardutils.numbers.NumberUtils;


public class ASpoofingResolver extends BasePlugin implements Resolver{

	protected String address = "127.0.0.1";
	protected Long ttl = 300l;

	public Message generateReply(Request request) throws Exception {

		Message query = request.getQuery();

		if(query.getQuestion().getType() == Type.A){

			log.debug("Resolver " + name + " spoofing reply for " + query.getQuestion().getName());

			TSIGRecord queryTSIG = query.getTSIG();
			TSIG tsig = null;
			if (queryTSIG != null) {
				tsig = systemInterface.getTSIG(queryTSIG.getName());
				if (tsig == null || tsig.verify(query, request.getRawQuery(), request.getRawQueryLength(), null) != Rcode.NOERROR) {
					return null;
				}
			}

			Message response = new Message(query.getHeader().getID());

			response.getHeader().setFlag(Flags.QR);
			if (query.getHeader().getFlag(Flags.RD)) {
				response.getHeader().setFlag(Flags.RD);
			}

			Record record =  Record.fromString(Name.fromString("@",query.getQuestion().getName()), query.getQuestion().getType(), query.getQuestion().getDClass(), ttl, this.address, query.getQuestion().getName());

			response.addRecord(record, Section.ANSWER);

			OPTRecord queryOPT = query.getOPT();

			int flags = 0;

			if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0) {
				flags = EagleDNS.FLAG_DNSSECOK;
			}

			if (queryOPT != null) {
				int optflags = (flags == EagleDNS.FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
				OPTRecord opt = new OPTRecord((short) 4096, Rcode.NOERROR, (byte) 0, optflags);
				response.addRecord(opt, Section.ADDITIONAL);
			}

			response.getHeader().setRcode(Rcode.NOERROR);

			return response;
		}

		return null;
	}

	public void setAddress(String address) {

		this.address = address;
	}


	public void setTtl(String ttlString) {

		Long ttl = NumberUtils.toLong(ttlString);

		if (ttl != null && ttl > 0) {

			this.ttl = ttl;

		} else {

			log.warn("Invalid ttl " + ttlString + " specified!");
		}
	}

	@Override
	public void init(String name) throws Exception {

		super.init(name);
		
		log.info("Resolver " + name + " configured to spoof replies using address " + address);		
	}
}
