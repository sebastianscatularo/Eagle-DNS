package se.unlogic.eagledns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.apache.log4j.Logger;


public class UDPConnection implements Runnable {

	private static final Logger log = Logger.getLogger(UDPConnection.class);

	private final EagleDNS eagleDNS;
	private final DatagramSocket socket;
	private final DatagramPacket inDataPacket;

	public UDPConnection(EagleDNS eagleDNS, DatagramSocket socket, DatagramPacket inDataPacket) {
		super();
		this.eagleDNS = eagleDNS;
		this.socket = socket;
		this.inDataPacket = inDataPacket;
	}

	public void run() {

		try{

			this.eagleDNS.UDPClient(socket, inDataPacket);

		}catch(Throwable e){

			log.error("Error processing UDP connection from " + inDataPacket.getSocketAddress());
		}
	}
}
