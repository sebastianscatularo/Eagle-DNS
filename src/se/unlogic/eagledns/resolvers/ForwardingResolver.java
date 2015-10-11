/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.resolvers;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Timer;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;

import se.unlogic.eagledns.EagleDNS;
import se.unlogic.eagledns.Request;
import se.unlogic.eagledns.plugins.BasePlugin;
import se.unlogic.standardutils.numbers.NumberUtils;
import se.unlogic.standardutils.time.MillisecondTimeUnits;
import se.unlogic.standardutils.time.TimeUtils;
import se.unlogic.standardutils.timer.RunnableTimerTask;

public class ForwardingResolver extends  BasePlugin implements Resolver, Runnable {

	protected String server;
	protected int port = 53;
	protected boolean tcp;

	protected Integer timeout;

	protected Integer maxerrors;
	protected Integer errorWindowsSize;

	protected String validationQuery = "google.com";
	protected int validationInterval = 5;

	protected SimpleResolver resolver;

	protected boolean online = true;

	private LinkedList<Long> errors = null;

	protected Lookup lookup;

	protected long requestsHandled;
	protected long requestsTimedout;

	protected String failoverResolverName;

	protected boolean replyOnTimeout = false;
	protected boolean replyOnUnsuccessfulLookup = false;
	
	protected Timer timer;

	@Override
	public void init(String name) throws Exception {
		
		super.init(name);

		if (server == null) {

			throw new RuntimeException("No server set!");
		}

		this.resolver = new SimpleResolver(server);
		this.resolver.setPort(port);

		if (timeout != null) {

			this.resolver.setTimeout(timeout);
		}

		log.info("Resolver " + name + " configured to forward queries to server " + server + ":" + port + " with timeout " + timeout + " sec.");

		if(this.failoverResolverName != null){

			log.info("Resolver " + name + " configured to act as failover for resolver " + failoverResolverName + " and will therefore only handle queries when resolver " + failoverResolverName + " is offline");
		}

		if (this.maxerrors != null && this.errorWindowsSize != null) {

			log.info("Resolver " + name + " has maxerrors set to " + maxerrors + " and errorWindowsSize set to " + errorWindowsSize + ", enabling failover detection");

			this.errors = new LinkedList<Long>();

			lookup = new Lookup(this.validationQuery);
			lookup.setCache(null);
			lookup.setResolver(resolver);
			lookup.setSearchPath((String[])null);
			
			this.timer = new Timer(name, true);
			
			timer.scheduleAtFixedRate(new RunnableTimerTask(this), 0, this.validationInterval * MillisecondTimeUnits.SECOND);
			
			log.info("Status monitoring thread for resolver " + name + " started");
		}
	}

	public Message generateReply(Request request) {

		if(this.failoverResolverName != null){

			Resolver resolver = systemInterface.getResolver(failoverResolverName);

			if(resolver == null){

				log.warn("Resolver " + name + " is configured to as failover for resolver " + failoverResolverName + " which cannot be found, ingnoring query " + EagleDNS.toString(request.getQuery().getQuestion()));
				return null;

			}else if(!(resolver instanceof ForwardingResolver)){

				log.warn("Resolver " + name + " is configured to as failover for resolver " + failoverResolverName + " which is not an instance of " + ForwardingResolver.class.getSimpleName() + ", ingnoring query " + EagleDNS.toString(request.getQuery().getQuestion()));
				return null;
			}

			if(((ForwardingResolver)resolver).online){

				log.debug("Resolver " + name + " ignoring query " + EagleDNS.toString(request.getQuery().getQuestion()) + " since resolver " + failoverResolverName + " is online");
				return null;
			}
		}

		if (this.online) {

			try {
				log.debug("Resolver " + name + " forwarding query " + EagleDNS.toString(request.getQuery().getQuestion()) + " to server " + server + ":" + port);

				Message response = resolver.send(request.getQuery());

				log.debug("Resolver " + name + " got response " + Rcode.string(response.getHeader().getRcode()) + " with " + response.getSectionArray(Section.ANSWER).length + " answer, " + response.getSectionArray(Section.AUTHORITY).length + " authoritative and " + response.getSectionArray(Section.ADDITIONAL).length + " additional records");

				Integer rcode = response.getHeader().getRcode();

				if (!replyOnUnsuccessfulLookup && (rcode == null || rcode == Rcode.NXDOMAIN || rcode == Rcode.SERVFAIL ||(rcode == Rcode.NOERROR && response.getSectionArray(Section.ANSWER).length == 0 && response.getSectionArray(Section.AUTHORITY).length == 0))) {

					return null;
				}

				requestsHandled++;

				return response;

			}catch(SocketTimeoutException e){

				requestsTimedout++;

				log.info("Timeout in resolver " + name + " while forwarding query " + EagleDNS.toString(request.getQuery().getQuestion()));

				if(replyOnTimeout){

					return EagleDNS.errorMessage(request.getQuery(), Rcode.SERVFAIL);
				}

			} catch (IOException e) {

				log.warn("Error " + e + " in resolver " + name + " while forwarding query " + EagleDNS.toString(request.getQuery().getQuestion()));

			} catch (RuntimeException e) {

				log.warn("Error " + e + " in resolver " + name + " while forwarding query " + EagleDNS.toString(request.getQuery().getQuestion()));
			}
		} else {

			log.debug("Resolver " + this.name + " is offline skipping query " + EagleDNS.toString(request.getQuery().getQuestion()));
		}

		return null;
	}

	public synchronized void processError() {

		long currentTime = System.currentTimeMillis();

		errors.add(currentTime);

		if (errors.size() > maxerrors) {

			errors.removeFirst();

			if (online && errors.getFirst() > (currentTime - (MillisecondTimeUnits.SECOND * errorWindowsSize))) {

				log.warn("Marking resolver " + name + " as offline after receiving " + maxerrors + " errors in " + TimeUtils.millisecondsToString((currentTime - errors.getFirst())));

				this.online = false;
			}
		}
	}

	public void setServer(String server) {

		this.server = server;
	}

	/**
	 * The connection time in seconds
	 * 
	 * @param stringTimeout
	 */
	public void setTimeout(String stringTimeout) {

		if (stringTimeout != null) {

			Integer timeout = NumberUtils.toInt(stringTimeout);

			if (timeout == null || timeout < 1) {

				log.warn("Invalid timeout " + stringTimeout + " specified");

			} else {

				this.timeout = timeout;
			}

		} else {

			this.timeout = null;
		}
	}

	public void setMaxerrors(String maxerrorsString) {

		if (maxerrorsString != null) {

			Integer maxerrors = NumberUtils.toInt(maxerrorsString);

			if (maxerrors == null || maxerrors < 1) {

				log.warn("Invalid max error value " + maxerrorsString + " specified");

			} else {

				this.maxerrors = maxerrors;
			}

		} else {

			this.maxerrors = null;
		}
	}

	public void setErrorWindowsSize(String errorWindowsSizeString) {

		if (errorWindowsSizeString != null) {

			Integer errorWindowsSize = NumberUtils.toInt(errorWindowsSizeString);

			if (errorWindowsSize == null || errorWindowsSize < 1) {

				log.warn("Invalid error window size " + errorWindowsSizeString + " specified");

			} else {

				this.errorWindowsSize = errorWindowsSize;
			}

		} else {

			this.errorWindowsSize = null;
		}
	}

	public void setPort(String portString) {

		Integer port = NumberUtils.toInt(portString);

		if (port != null && port >= 1 && port <= 65536) {

			this.port = port;

		} else {

			log.warn("Invalid port " + portString + " specified! (sticking to default value " + this.port + ")");
		}
	}

	public void setTcp(String tcp) {

		this.tcp = Boolean.parseBoolean(tcp);
	}

	public void setValidationQuery(String validationQuery) {

		this.validationQuery = validationQuery;
	}


	public void setValidationInterval(String validationIntervalString) {

		Integer validationInterval = NumberUtils.toInt(validationIntervalString);

		if (validationInterval != null && validationInterval > 0) {

			this.validationInterval = validationInterval;

		} else {

			log.warn("Invalid validation interval " + validationIntervalString + " specified!");
		}
	}

	public void run() {
		
		try {
			lookup.run();
			
			if(online){

				if(lookup.getResult() == Lookup.SUCCESSFUL){

					log.debug("Resolver " + this.name + " is still up, got response " + Rcode.string(lookup.getResult()) + " from upstream server for query " + validationQuery);

				}else{


					log.warn("Resolver " + this.name + " got unsuccessful response " + Rcode.string(lookup.getResult()) + " from upstream server for query " + validationQuery);
					this.processError();
				}

			}else{

				if(lookup.getResult() == Lookup.SUCCESSFUL){

					log.warn("Marking resolver " + this.name + " as online after getting succesful response from query for " + this.validationQuery);
					this.online = true;

				}else{

					log.debug("Resolver " + this.name + " is still down, got response " + Rcode.string(lookup.getResult()) + " from upstream server for query " + validationQuery);
				}
			}

		} catch (RuntimeException e) {

			if(online){

				log.warn("Marking resolver " + this.name + " as offline after getting error " + e + " when trying to resolve query " + validationQuery);
				this.online = false;

			}else{

				log.debug("Resolver " + this.name + " is still down, got error " + e + " when trying to resolve " + this.validationQuery);
			}
		}
	}


	public long getRequestsHandled() {

		return requestsHandled;
	}


	public long getRequestsTimedout() {

		return requestsTimedout;
	}

	public void setFailoverForResolver(String resolverName){

		this.failoverResolverName = resolverName;
	}


	public void setReplyOnTimeout(String replyOnTimeout) {

		this.replyOnTimeout = Boolean.parseBoolean(replyOnTimeout);
	}


	public void setReplyOnUnsuccessfulLookup(String replyOnUnsuccessfulLookup) {

		this.replyOnUnsuccessfulLookup = Boolean.parseBoolean(replyOnUnsuccessfulLookup);
	}

	@Override
	public void shutdown() throws Exception {

		if(this.timer != null){
			
			timer.cancel();
		}
		
		super.shutdown();
	}
}
