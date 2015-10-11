/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.test;

import java.io.File;
import java.io.IOException;

import org.xbill.DNS.Master;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;


public class ZoneDissasembler {

	public static void main(String[] args) throws TextParseException, IOException {

		File zoneFile = new File("zones/unlogic.se");

		Master master = new Master(zoneFile.getPath(),Name.fromString(zoneFile.getName(), Name.root));

		Record record = master._nextRecord();

		while(record != null){

			System.out.println("Class: " + record.getClass());
			System.out.println("Name: " + record.getName());
			System.out.println("toString: " + record.toString());
			System.out.println();

			record = master._nextRecord();
		}
	}

}
