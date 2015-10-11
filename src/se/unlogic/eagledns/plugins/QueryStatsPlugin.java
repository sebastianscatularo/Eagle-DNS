/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.plugins;

import java.io.File;
import java.util.Timer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import se.unlogic.standardutils.numbers.NumberUtils;
import se.unlogic.standardutils.time.MillisecondTimeUnits;
import se.unlogic.standardutils.timer.RunnableTimerTask;
import se.unlogic.standardutils.xml.XMLParser;
import se.unlogic.standardutils.xml.XMLUtils;


public class QueryStatsPlugin extends BasePlugin implements Runnable{
	
	protected long savingInterval = 60;
	
	protected String filePath;
	protected File file;
	
	protected long tcpOffset;
	protected long udpOffset;
	protected long tcpRejectedOffset;
	protected long udpRejectedOffset;	
	
	protected Timer timer;
	
	@Override
	public void init(String name) throws Exception {

		super.init(name);
		
		if(filePath == null){
			
			throw new RuntimeException("No file set!");
			
		}
		
		file = new File(filePath);
		
		if(file.exists()){
			
			log.info("Plugin " + name + " reading previously saved statistics from file " + file.getAbsolutePath());
			
			try {
				XMLParser settingNode = new XMLParser(file);
				
				tcpOffset = settingNode.getLong("/Statistics/TCPQueryCount");
				udpOffset = settingNode.getLong("/Statistics/UDPQueryCount");
				tcpRejectedOffset = settingNode.getLong("/Statistics/RejectedTCPConnections");
				udpRejectedOffset = settingNode.getLong("/Statistics/RejectedUDPConnections");				
				
			} catch (Exception e) {

				log.error("Error reading previously saved statistics from file " + file.getAbsolutePath(),e);
			}
			
		}else{
			
			log.info("Plugin " + name + " found no previously saved statistics,, creating new file on first save");
		}
		
		timer = new Timer(true);
		
		timer.schedule(new RunnableTimerTask(this), savingInterval * MillisecondTimeUnits.SECOND, savingInterval * MillisecondTimeUnits.SECOND);
	}
	
	@Override
	public void shutdown() throws Exception {

		timer.cancel();
		
		this.run();
		
		super.shutdown();
	}

	
	public void setSavingInterval(String savingInterval) {
	
		Long interval = NumberUtils.toLong(savingInterval);
		
		if(interval == null || interval < 1){
			
			log.error("Invalid saving interval value " + savingInterval + " specified, falling back to default value of " + this.savingInterval + " sec");
		
		}else{
		
			this.savingInterval = interval;
		}
	}

	public void run() {

		try{
			Document doc = XMLUtils.createDomDocument();
			
			Element statisticsElement = doc.createElement("Statistics");
			doc.appendChild(statisticsElement);
			
			XMLUtils.appendNewElement(doc, statisticsElement, "TCPQueryCount", tcpOffset + systemInterface.getCompletedTCPQueryCount());
			XMLUtils.appendNewElement(doc, statisticsElement, "UDPQueryCount", udpOffset + systemInterface.getCompletedUDPQueryCount());
			
			XMLUtils.appendNewElement(doc, statisticsElement, "RejectedTCPConnections", tcpRejectedOffset + systemInterface.getRejectedTCPConnections());
			XMLUtils.appendNewElement(doc, statisticsElement, "RejectedUDPConnections", udpRejectedOffset + systemInterface.getRejectedUDPConnections());			
			
			XMLUtils.writeXMLFile(doc, file, true, "UTF-8");
			
			log.debug("Plugin " + name + " successfully saved query statistics to file " + file.getAbsolutePath());
			
		}catch(Throwable t){
			
			log.error("Plugin " + name + " unable to save query statistics to file " + file.getAbsolutePath(),t);
		}
	}

	public void setFilePath(String filePath) {
	
		this.filePath = filePath;
	}
}
