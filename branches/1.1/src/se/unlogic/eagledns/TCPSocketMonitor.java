/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;

import se.unlogic.standardutils.net.SocketUtils;


public class TCPSocketMonitor extends Thread {

	private Logger log = Logger.getLogger(this.getClass());

	private final EagleDNS eagleDNS;
	private final InetAddress addr;
	private final int port;
	private final ServerSocket serverSocket;

	public TCPSocketMonitor(EagleDNS eagleDNS, final InetAddress addr, final int port) throws IOException {
		super();
		this.eagleDNS = eagleDNS;
		this.addr = addr;
		this.port = port;

		serverSocket = new ServerSocket(port, 128, addr);

		this.setDaemon(true);
		this.start();
	}

	@Override
	public void run() {

		log.info("Starting TCP socket monitor on address " + getAddressAndPort());

		while (eagleDNS.getStatus() == Status.STARTED) {

			Socket socket = null;
			
			try {

				socket = serverSocket.accept();

				log.debug("TCP connection from " + socket.getRemoteSocketAddress());

				if(eagleDNS.getStatus() == Status.STARTED){
					
					this.eagleDNS.getTcpThreadPool().execute(new TCPConnection(eagleDNS, socket));	
				}
				

			} catch (RejectedExecutionException e) {
				
				if(eagleDNS.getStatus() == Status.STARTED){
					
					log.warn("TCP thread pool exausted, rejecting connection from " + socket.getRemoteSocketAddress());					
					eagleDNS.incrementRejectedTCPConnections();	
				}
				
				SocketUtils.closeSocket(socket);
				
			} catch (SocketException e) {

				//This is usally thrown on shutdown
				log.debug("SocketException thrown from TCP socket on address " + getAddressAndPort() + ", " + e);
				SocketUtils.closeSocket(socket);

			} catch (IOException e) {

				log.info("IOException thrown by TCP socket on address " + getAddressAndPort() + ", " + e);
				SocketUtils.closeSocket(socket);
			
			} catch (Throwable t) {

				log.info("Throwable thrown by TCP socket on address " + getAddressAndPort(),t);
				SocketUtils.closeSocket(socket);
			}
		}

		log.info("TCP socket monitor on address " + getAddressAndPort() + " shutdown");
	}


	public InetAddress getAddr() {

		return addr;
	}


	public int getPort() {

		return port;
	}


	public ServerSocket getServerSocket() {

		return serverSocket;
	}

	public void closeSocket() throws IOException{

		log.info("Closing TCP socket monitor on address " + getAddressAndPort() + "...");

		this.serverSocket.close();
	}

	public String getAddressAndPort(){

		return addr.getHostAddress() + ":" + port;
	}
}
