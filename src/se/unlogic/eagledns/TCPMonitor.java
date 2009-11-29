package se.unlogic.eagledns;

import java.net.InetAddress;

import org.apache.log4j.Logger;


public class TCPMonitor implements Runnable {

	private Logger log = Logger.getLogger(this.getClass());

	private final EagleDNS eagleDNS;
	private final InetAddress addr;
	private final int port;

	public TCPMonitor(EagleDNS eagleDNS, final InetAddress addr, final int port) {
		super();
		this.eagleDNS = eagleDNS;
		this.addr = addr;
		this.port = port;
	}

	public void run() {

		log.debug("Starting TCP monitor on address " + addr + ":" + port);

		eagleDNS.serveTCP(addr, port);
	}
}
