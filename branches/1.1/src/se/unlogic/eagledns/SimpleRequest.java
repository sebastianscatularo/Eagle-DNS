/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns;

import java.net.Socket;
import java.net.SocketAddress;

import org.xbill.DNS.Message;

public class SimpleRequest implements Request {

	private SocketAddress socketAddress;
	private Message query;
	private byte[] rawQuery;
	private int rawQueryLength;
	private Socket socket;

	public SimpleRequest(SocketAddress socketAddress, Message query, byte[] rawQuery, int rawQueryLength, Socket socket) {

		super();
		this.socketAddress = socketAddress;
		this.query = query;
		this.rawQuery = rawQuery;
		this.rawQueryLength = rawQueryLength;
		this.socket = socket;
	}

	public SocketAddress getSocketAddress() {

		return socketAddress;
	}

	public Message getQuery() {

		return query;
	}

	public byte[] getRawQuery() {

		return rawQuery;
	}

	public int getRawQueryLength() {

		return rawQueryLength;
	}

	public Socket getSocket() {

		return socket;
	}
}
