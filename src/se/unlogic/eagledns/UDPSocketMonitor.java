/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;


public class UDPSocketMonitor extends Thread {

	private Logger log = Logger.getLogger(this.getClass());

	private final EagleDNS eagleDNS;
	private final InetAddress addr;
	private final int port;
	private static final short UDP_LENGTH = 512;
	private final DatagramSocket socket;

	public UDPSocketMonitor(EagleDNS eagleDNS, final InetAddress addr, final int port) throws SocketException {
		super();
		this.eagleDNS = eagleDNS;
		this.addr = addr;
		this.port = port;

		socket = new DatagramSocket(port, addr);

		this.setDaemon(true);
		this.start();
	}

	@Override
	public void run() {

		log.info("Starting UDP socket monitor on address " + this.getAddressAndPort());

		while (eagleDNS.getStatus() == Status.STARTING || eagleDNS.getStatus() == Status.STARTED) {

			DatagramPacket indp = null;
			
			try {

				byte[] in = new byte[UDP_LENGTH];
				indp = new DatagramPacket(in, in.length);

				indp.setLength(in.length);
				socket.receive(indp);

				log.debug("UDP connection from " + indp.getSocketAddress());

				if(eagleDNS.getStatus() == Status.STARTING || eagleDNS.getStatus() == Status.STARTED){

					this.eagleDNS.getUdpThreadPool().execute(new UDPConnection(eagleDNS, socket, indp));
				}

			}catch (RejectedExecutionException e) {
				
				if(eagleDNS.getStatus() == Status.STARTING || eagleDNS.getStatus() == Status.STARTED){
					
					log.warn("UDP thread pool exausted, rejecting connection from " + indp.getSocketAddress());
					eagleDNS.incrementRejectedUDPConnections();
				}
				
			} catch (SocketException e) {

				//This is usally thrown on shutdown
				log.debug("SocketException thrown from UDP socket on address " + this.getAddressAndPort() + ", " + e);

			} catch (IOException e) {

				log.info("IOException thrown by UDP socket on address " + this.getAddressAndPort() + ", " + e);
				
			}catch (Throwable t) {

				log.info("Throwable thrown by UDO socket on address " + getAddressAndPort(),t);
			}
		}

		log.info("UDP socket monitor on address " + getAddressAndPort() + " shutdown");
	}

	public void closeSocket() throws IOException{

		log.info("Closing TCP socket monitor on address " + getAddressAndPort() + "...");

		this.socket.close();
	}

	public String getAddressAndPort(){

		return addr.getHostAddress() + ":" + port;
	}
}
