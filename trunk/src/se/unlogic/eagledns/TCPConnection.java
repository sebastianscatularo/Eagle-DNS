package se.unlogic.eagledns;

import java.net.Socket;

import org.apache.log4j.Logger;


public class TCPConnection implements Runnable {

	private static Logger log = Logger.getLogger(TCPConnection.class);

	private EagleDNS eagleDNS;
	private Socket socket;

	public TCPConnection(EagleDNS eagleDNS, Socket socket) {
		super();
		this.eagleDNS = eagleDNS;
		this.socket = socket;
	}

	public void run() {

		try{

			this.eagleDNS.TCPclient(socket);

		}catch(Throwable e){

			log.error("Error processing TCP connection from " + socket.getRemoteSocketAddress() + ":" + socket.getPort());
		}
	}
}
