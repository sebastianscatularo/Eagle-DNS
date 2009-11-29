package se.unlogic.eagledns;

import java.net.InetAddress;

import org.apache.log4j.Logger;


public class UDPMonitor implements Runnable {

	private Logger log = Logger.getLogger(this.getClass());

	private final EagleDNS eagleDNS;
	private final InetAddress addr;
	private final int port;

	public UDPMonitor(EagleDNS eagleDNS, final InetAddress addr, final int port) {
		super();
		this.eagleDNS = eagleDNS;
		this.addr = addr;
		this.port = port;
	}

	public void run() {

		log.debug("Starting UDP monitor on address " + addr + ":" + port);

		eagleDNS.serveUDP(addr, port);
	}
}
