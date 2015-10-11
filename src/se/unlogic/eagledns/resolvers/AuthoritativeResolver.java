/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.resolvers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TSIGRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.Zone;

import se.unlogic.eagledns.EagleDNS;
import se.unlogic.eagledns.Request;
import se.unlogic.eagledns.plugins.BasePlugin;
import se.unlogic.standardutils.net.SocketUtils;

/**
 * This resolver is strictly authorative and uses Eagle DNS internal cache of authorative zones to answer queries.
 * 
 * @author Robert "Unlogic" Olofsson (unlogic@unlogic.se)
 * 
 */
public class AuthoritativeResolver extends BasePlugin implements Resolver{

	public Message generateReply(Request request) throws Exception {
		
		Message query = request.getQuery();
		Record queryRecord = query.getQuestion();
		
		if(queryRecord == null){
			return null;
		}
		
		Name name = queryRecord.getName();
		Zone zone = findBestZone(name);
		
		if(zone != null){
		
			log.debug("Resolver " + this.name + " processing request for " + name + ", matching zone found ");
			
			Header header;
			// boolean badversion;
			int flags = 0;

			header = query.getHeader();
			if (header.getFlag(Flags.QR)) {
				return null;
			}
			if (header.getRcode() != Rcode.NOERROR) {
				return null;
			}
			if (header.getOpcode() != Opcode.QUERY) {
				return null;
			}

			TSIGRecord queryTSIG = query.getTSIG();
			TSIG tsig = null;
			if (queryTSIG != null) {
				tsig = systemInterface.getTSIG(queryTSIG.getName());
				if (tsig == null || tsig.verify(query, request.getRawQuery(), request.getRawQueryLength(), null) != Rcode.NOERROR) {
					return null;
				}
			}

			OPTRecord queryOPT = query.getOPT();
			//		if (queryOPT != null && queryOPT.getVersion() > 0) {
			//			// badversion = true;
			//		}

			if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0) {
				flags = EagleDNS.FLAG_DNSSECOK;
			}

			Message response = new Message(query.getHeader().getID());
			response.getHeader().setFlag(Flags.QR);
			if (query.getHeader().getFlag(Flags.RD)) {
				response.getHeader().setFlag(Flags.RD);
			}

			response.addRecord(queryRecord, Section.QUESTION);

			int type = queryRecord.getType();
			int dclass = queryRecord.getDClass();
			if (type == Type.AXFR && request.getSocket() != null) {
				return doAXFR(name, query, tsig, queryTSIG, request.getSocket());
			}
			if (!Type.isRR(type) && type != Type.ANY) {
				return null;
			}

			byte rcode = addAnswer(response, name, type, dclass, 0, flags,zone);

			if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
				return EagleDNS.errorMessage(query, rcode);
			}

			addAdditional(response, flags);

			if (queryOPT != null) {
				int optflags = (flags == EagleDNS.FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
				OPTRecord opt = new OPTRecord((short) 4096, rcode, (byte) 0, optflags);
				response.addRecord(opt, Section.ADDITIONAL);
			}

			response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);

			return response;
			
		}else{
			
			log.debug("Resolver " + this.name + " ignoring request for " + name + ", no matching zone found");
			
			return null;
		}
	}

	private final void addAdditional(Message response, int flags) {

		addAdditional2(response, Section.ANSWER, flags);
		addAdditional2(response, Section.AUTHORITY, flags);
	}

	private byte addAnswer(Message response, Name name, int type, int dclass, int iterations, int flags, Zone zone) {

		SetResponse sr;
		byte rcode = Rcode.NOERROR;

		if (iterations > 6) {
			return Rcode.NOERROR;
		}

		if (type == Type.SIG || type == Type.RRSIG) {
			type = Type.ANY;
			flags |= EagleDNS.FLAG_SIGONLY;
		}

		if(zone == null){
			
			zone = findBestZone(name);
		}
		
		if (zone != null) {
			sr = zone.findRecords(name, type);

			if (sr.isNXDOMAIN()) {
				response.getHeader().setRcode(Rcode.NXDOMAIN);
				if (zone != null) {
					addSOA(response, zone);
					if (iterations == 0) {
						response.getHeader().setFlag(Flags.AA);
					}
				}
				rcode = Rcode.NXDOMAIN;
			} else if (sr.isNXRRSET()) {
				if (zone != null) {
					addSOA(response, zone);
					if (iterations == 0) {
						response.getHeader().setFlag(Flags.AA);
					}
				}
			} else if (sr.isDelegation()) {
				RRset nsRecords = sr.getNS();
				addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
			} else if (sr.isCNAME()) {
				CNAMERecord cname = sr.getCNAME();
				RRset rrset = new RRset(cname);
				addRRset(name, response, rrset, Section.ANSWER, flags);
				if (zone != null && iterations == 0) {
					response.getHeader().setFlag(Flags.AA);
				}
				rcode = addAnswer(response, cname.getTarget(), type, dclass, iterations + 1, flags,null);
			} else if (sr.isDNAME()) {
				DNAMERecord dname = sr.getDNAME();
				RRset rrset = new RRset(dname);
				addRRset(name, response, rrset, Section.ANSWER, flags);
				Name newname;
				try {
					newname = name.fromDNAME(dname);
				} catch (NameTooLongException e) {
					return Rcode.YXDOMAIN;
				}
				rrset = new RRset(new CNAMERecord(name, dclass, 0, newname));
				addRRset(name, response, rrset, Section.ANSWER, flags);
				if (zone != null && iterations == 0) {
					response.getHeader().setFlag(Flags.AA);
				}
				rcode = addAnswer(response, newname, type, dclass, iterations + 1, flags,null);
			} else if (sr.isSuccessful()) {
				RRset[] rrsets = sr.answers();
				for (RRset rrset : rrsets) {
					addRRset(name, response, rrset, Section.ANSWER, flags);
				}
				if (zone != null) {
					addNS(response, zone, flags);
					if (iterations == 0) {
						response.getHeader().setFlag(Flags.AA);
					}
				}
			}
		}

		return rcode;
	}

	private Message doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig, Socket socket) {

		boolean first = true;

		Zone zone = this.findBestZone(name);

		if (zone == null) {

			return EagleDNS.errorMessage(query, Rcode.REFUSED);

		}

		// Check that the IP requesting the AXFR is present as a NS in this zone
		boolean axfrAllowed = false;

		Iterator<?> nsIterator = zone.getNS().rrs();

		while (nsIterator.hasNext()) {

			NSRecord record = (NSRecord) nsIterator.next();

			try {
				String nsIP = InetAddress.getByName(record.getTarget().toString()).getHostAddress();

				if (socket.getInetAddress().getHostAddress().equals(nsIP)) {

					axfrAllowed = true;
					break;
				}

			} catch (UnknownHostException e) {

				log.warn("Unable to resolve hostname of nameserver " + record.getTarget() + " in zone " + zone.getOrigin() + " while processing AXFR request from " + socket.getRemoteSocketAddress());
			}
		}

		if (!axfrAllowed) {
			log.warn("AXFR request of zone " + zone.getOrigin() + " from " + socket.getRemoteSocketAddress() + " refused!");
			return EagleDNS.errorMessage(query, Rcode.REFUSED);
		}

		Iterator<?> it = zone.AXFR();

		try {
			DataOutputStream dataOut;
			dataOut = new DataOutputStream(socket.getOutputStream());
			int id = query.getHeader().getID();
			while (it.hasNext()) {
				RRset rrset = (RRset) it.next();
				Message response = new Message(id);
				Header header = response.getHeader();
				header.setFlag(Flags.QR);
				header.setFlag(Flags.AA);
				addRRset(rrset.getName(), response, rrset, Section.ANSWER, EagleDNS.FLAG_DNSSECOK);
				if (tsig != null) {
					tsig.applyStream(response, qtsig, first);
					qtsig = response.getTSIG();
				}
				first = false;
				byte[] out = response.toWire();
				dataOut.writeShort(out.length);
				dataOut.write(out);
			}
		} catch (IOException ex) {
			log.warn("AXFR failed", ex);
		} finally {
			SocketUtils.closeSocket(socket);
		}

		return null;
	}

	private final void addSOA(Message response, Zone zone) {

		response.addRecord(zone.getSOA(), Section.AUTHORITY);
	}

	private final void addNS(Message response, Zone zone, int flags) {

		RRset nsRecords = zone.getNS();
		addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
	}

	private void addGlue(Message response, Name name, int flags) {

		RRset a = findExactMatch(name, Type.A, DClass.IN, true);
		if (a == null) {
			return;
		}
		addRRset(name, response, a, Section.ADDITIONAL, flags);
	}

	private void addAdditional2(Message response, int section, int flags) {

		Record[] records = response.getSectionArray(section);
		for (Record r : records) {
			Name glueName = r.getAdditionalName();
			if (glueName != null) {
				addGlue(response, glueName, flags);
			}
		}
	}

	private RRset findExactMatch(Name name, int type, int dclass, boolean glue) {

		Zone zone = findBestZone(name);

		if (zone != null) {
			return zone.findExactMatch(name, type);
		}

		return null;
	}

	private void addRRset(Name name, Message response, RRset rrset, int section, int flags) {

		for (int s = 1; s <= section; s++) {
			if (response.findRRset(name, rrset.getType(), s)) {
				return;
			}
		}
		if ((flags & EagleDNS.FLAG_SIGONLY) == 0) {
			Iterator<?> it = rrset.rrs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild()) {
					r = r.withName(name);
				}
				response.addRecord(r, section);
			}
		}
		if ((flags & (EagleDNS.FLAG_SIGONLY | EagleDNS.FLAG_DNSSECOK)) != 0) {
			Iterator<?> it = rrset.sigs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild()) {
					r = r.withName(name);
				}
				response.addRecord(r, section);
			}
		}
	}

	private Zone findBestZone(Name name) {

		Zone foundzone = systemInterface.getZone(name);

		if (foundzone != null) {
			return foundzone;
		}

		int labels = name.labels();

		for (int i = 1; i < labels; i++) {

			Name tname = new Name(name, i);
			foundzone = systemInterface.getZone(tname);

			if (foundzone != null) {
				return foundzone;
			}
		}

		return null;
	}
}
