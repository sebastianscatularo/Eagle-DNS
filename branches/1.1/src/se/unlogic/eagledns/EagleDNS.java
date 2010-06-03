package se.unlogic.eagledns;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.xbill.DNS.Address;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TSIGRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.Zone;

import se.unlogic.eagledns.plugins.Plugin;
import se.unlogic.eagledns.resolvers.Resolver;
import se.unlogic.eagledns.zoneproviders.ZoneProvider;
import se.unlogic.standardutils.datatypes.SimpleEntry;
import se.unlogic.standardutils.reflection.ReflectionUtils;
import se.unlogic.standardutils.settings.SettingNode;
import se.unlogic.standardutils.settings.XMLSettingNode;
import se.unlogic.standardutils.string.StringUtils;
import se.unlogic.standardutils.time.MillisecondTimeUnits;
import se.unlogic.standardutils.timer.RunnableTimerTask;

/**
 * EagleDNS copyright Robert "Unlogic" Olofsson (unlogic@unlogic.se)
 * 
 * Based on the jnamed class from the dnsjava project (http://www.dnsjava.org/) copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)
 * 
 * @author Robert "Unlogic" Olofsson Contributions by Michael Neale, Red Hat (JBoss division)
 */

public class EagleDNS implements Runnable, SystemInterface {

	public static final String VERSION = "Eagle DNS 1.1 beta 4, time limited version for ComAbility";

	public final long startTime;

	public static final int FLAG_DNSSECOK = 1;
	public static final int FLAG_SIGONLY = 2;

	private final Logger log = Logger.getLogger(this.getClass());

	private ConcurrentHashMap<Name, CachedPrimaryZone> primaryZoneMap = new ConcurrentHashMap<Name, CachedPrimaryZone>();
	private ConcurrentHashMap<Name, CachedSecondaryZone> secondaryZoneMap = new ConcurrentHashMap<Name, CachedSecondaryZone>();
	
	private final HashMap<Name, TSIG> TSIGs = new HashMap<Name, TSIG>();
	
	private final HashMap<String, ZoneProvider> zoneProviders = new HashMap<String, ZoneProvider>();
	private final ArrayList<Entry<String, Resolver>> resolvers = new ArrayList<Entry<String, Resolver>>();
	private final HashMap<String, Plugin> plugins = new HashMap<String, Plugin>();

	private int tcpThreadPoolSize = 20;
	private int udpThreadPoolSize = 20;

	private int tcpThreadPoolShutdownTimeout = 60;
	private int udpThreadPoolShutdownTimeout = 60;

	private ArrayList<TCPSocketMonitor> tcpMonitorThreads = new ArrayList<TCPSocketMonitor>();
	private ArrayList<UDPSocketMonitor> udpMonitorThreads = new ArrayList<UDPSocketMonitor>();

	private ThreadPoolExecutor tcpThreadPool;
	private ThreadPoolExecutor udpThreadPool;

	private int axfrTimeout = 60;

	private Timer secondaryZoneUpdateTimer;
	private RunnableTimerTask timerTask;

	private boolean shutdown = false;

	private int defaultResponse;

	public EagleDNS(String configFilePath) throws UnknownHostException {

		if(System.currentTimeMillis() > 1276430907251l){

			System.out.println("Time limit expired, contact Robert Olofsson (unlogic@unlogic.se) +46703898218 for more information.");
			System.exit(0);
		}

		this.startTime = System.currentTimeMillis();

		DOMConfigurator.configure("conf/log4j.xml");

		System.out.println(VERSION + " starting...");
		log.fatal(VERSION + " starting...");

		XMLSettingNode configFile;

		try {
			log.debug("Parsing config file...");
			configFile = new XMLSettingNode(configFilePath);

		} catch (Exception e) {

			log.fatal("Unable to open config file " + configFilePath + ", aborting startup!");
			System.out.println("Unable to open config file " + configFilePath + ", aborting startup!");
			return;
		}

		boolean requireZones = configFile.getBoolean("/Config/System/RequireZones");

		String defaultResponse = configFile.getString("/Config/System/DefaultResponse");

		if (defaultResponse.equalsIgnoreCase("NOERROR")) {

			this.defaultResponse = Rcode.NOERROR;

		} else if (defaultResponse.equalsIgnoreCase("NXDOMAIN")) {

			this.defaultResponse = Rcode.NXDOMAIN;

		} else if (StringUtils.isEmpty(defaultResponse)) {

			log.fatal("No default response found, aborting startup!");
			System.out.println("No default response found, aborting startup!");
			return;

		} else {

			log.fatal("Invalid default response '" + defaultResponse + "' found, aborting startup!");
			System.out.println("Invalid default response '" + defaultResponse + "' found, aborting startup!");
			return;
		}

		List<Integer> ports = configFile.getIntegers("/Config/System/Port");

		if (ports.isEmpty()) {

			log.debug("No ports found in config file " + configFilePath + ", using default port 53");
			ports.add(new Integer(53));
		}

		List<InetAddress> addresses = new ArrayList<InetAddress>();
		List<String> addressStrings = configFile.getStrings("/Config/System/Address");

		if (addressStrings.isEmpty()) {

			log.debug("No addresses found in config, listening on all addresses (0.0.0.0)");
			addresses.add(Address.getByAddress("0.0.0.0"));

		} else {

			for (String addressString : addressStrings) {

				try {

					addresses.add(Address.getByAddress(addressString));

				} catch (UnknownHostException e) {

					log.error("Invalid address " + addressString + " specified in config file, skipping address " + e);
				}
			}

			if (addresses.isEmpty()) {

				log.fatal("None of the " + addressStrings.size() + " addresses specified in the config file are valid, aborting startup!\n" + "Correct the addresses or remove them from the config file if you want to listen on all interfaces.");
				System.out.println("None of the " + addressStrings.size() + " addresses specified in the config file are valid, aborting startup!\n" + "Correct the addresses or remove them from the config file if you want to listen on all interfaces.");
			}
		}

		Integer tcpThreadPoolSize = configFile.getInteger("/Config/System/TCPThreadPoolSize");

		if (tcpThreadPoolSize != null) {

			log.debug("Setting TCP thread pool size to " + tcpThreadPoolSize);
			this.tcpThreadPoolSize = tcpThreadPoolSize;
		}

		Integer tcpThreadPoolShutdownTimeout = configFile.getInteger("/Config/System/TCPThreadPoolShutdownTimeout");

		if (tcpThreadPoolShutdownTimeout != null) {

			log.debug("Setting TCP thread pool shutdown timeout to " + tcpThreadPoolSize + " seconds");
			this.tcpThreadPoolShutdownTimeout = tcpThreadPoolShutdownTimeout;
		}

		Integer udpThreadPoolSize = configFile.getInteger("/Config/System/UDPThreadPoolSize");

		if (udpThreadPoolSize != null) {

			log.debug("Setting UDP thread pool size to " + udpThreadPoolSize);
			this.udpThreadPoolSize = udpThreadPoolSize;
		}

		Integer udpThreadPoolShutdownTimeout = configFile.getInteger("/Config/System/UDPThreadPoolShutdownTimeout");

		if (udpThreadPoolShutdownTimeout != null) {

			log.debug("Setting UDP thread pool shutdown timeout to " + udpThreadPoolSize + " seconds");
			this.udpThreadPoolShutdownTimeout = udpThreadPoolShutdownTimeout;
		}

		Integer axfrTimeout = configFile.getInteger("/Config/System/AXFRTimeout");

		if (axfrTimeout != null) {

			log.debug("Setting AXFR timeout to " + axfrTimeout);
			this.axfrTimeout = axfrTimeout;
		}

		// TODO TSIG stuff

		List<XMLSettingNode> zoneProviderElements = configFile.getSettings("/Config/ZoneProviders/ZoneProvider");

		for (XMLSettingNode settingNode : zoneProviderElements) {

			String name = settingNode.getString("Name");

			if (StringUtils.isEmpty(name)) {

				log.error("ZoneProvider element with no name set found in config, ignoring element.");
				continue;
			}

			String className = settingNode.getString("Class");

			if (StringUtils.isEmpty(className)) {

				log.error("ZoneProvider element with no class set found in config, ignoring element.");
				continue;
			}

			try {

				log.debug("Instantiating zone provider " + name + " (" + className + ")");

				ZoneProvider zoneProvider = (ZoneProvider) Class.forName(className).newInstance();

				log.debug("Zone provider " + name + " successfully instantiated");

				List<XMLSettingNode> propertyElements = settingNode.getSettings("Properties/Property");

				for (SettingNode propertyElement : propertyElements) {

					String propertyName = propertyElement.getString("@name");

					if (StringUtils.isEmpty(propertyName)) {

						log.error("Property element with no name set found in config for zone provider " + name + ", ignoring element");
						continue;
					}

					String value = propertyElement.getString(".");

					log.debug("Found value " + value + " for property " + propertyName);

					try {
						Method method = zoneProvider.getClass().getMethod("set" + StringUtils.toFirstLetterUppercase(propertyName), String.class);

						ReflectionUtils.fixMethodAccess(method);

						log.debug("Setting property " + propertyName);

						try {

							method.invoke(zoneProvider, value);

						} catch (IllegalArgumentException e) {

							log.error("Unable to set property " + propertyName + " on zone provider " + name + " (" + className + ")", e);
							System.out.println("Unable to set property " + propertyName + " on zone provider " + name + " (" + className + ")");

						} catch (InvocationTargetException e) {

							log.error("Unable to set property " + propertyName + " on zone provider " + name + " (" + className + ")", e);
							System.out.println("Unable to set property " + propertyName + " on zone provider " + name + " (" + className + ")");
						}

					} catch (SecurityException e) {

						log.error("Unable to find matching setter method for property " + propertyName + " in zone provider " + name + " (" + className + ")", e);
						System.out.println("Unable to find matching setter method for property " + propertyName + " in zone provider " + name + " (" + className + ")");

					} catch (NoSuchMethodException e) {

						log.error("Unable to find matching setter method for property " + propertyName + " in zone provider " + name + " (" + className + ")", e);
						System.out.println("Unable to find matching setter method for property " + propertyName + " in zone provider " + name + " (" + className + ")");
					}
				}

				try {

					if (zoneProvider instanceof ZoneProviderUpdatable) {
						((ZoneProviderUpdatable) zoneProvider).setChangeListener(new ZoneChangeCallback() {

							public void zoneDataChanged() {

								reloadZones();
							}
						});
					}

					zoneProvider.init(name);

					log.info("Zone provider " + name + " (" + className + ") successfully initialized!");
					System.out.println("Zone provider " + name + " (" + className + ") successfully initialized!");

					this.zoneProviders.put(name, zoneProvider);

				} catch (Throwable e) {

					log.error("Error initializing zone provider " + name + " (" + className + ")", e);
					System.out.println("Error initializing zone provider " + name + " (" + className + ")");
				}

			} catch (InstantiationException e) {

				log.error("Unable to create instance of class " + className + " for zone provider " + name, e);
				System.out.println("Unable to create instance of class " + className + " for zone provider " + name);

			} catch (IllegalAccessException e) {

				log.error("Unable to create instance of class " + className + " for zone provider " + name, e);
				System.out.println("Unable to create instance of class " + className + " for zone provider " + name);

			} catch (ClassNotFoundException e) {

				log.error("Unable to create instance of class " + className + " for zone provider " + name, e);
				System.out.println("Unable to create instance of class " + className + " for zone provider " + name);
			}
		}

		if (requireZones && zoneProviders.isEmpty()) {
			log.fatal("No started zone providers found, aborting startup! (disable the /Config/System/RequireZones property in configuration file if you wish to proceed without any zones or zone provider)");
			System.out.println("No started zone providers found, aborting startup! (disable the /Config/System/RequireZones property in configuration file if you wish to proceed without any zones or zone provider)");
			return;
		}

		this.reloadZones();

		if (requireZones && this.primaryZoneMap.isEmpty() && this.secondaryZoneMap.isEmpty()) {

			log.fatal("No started zone providers found, aborting startup! (disable the /Config/System/RequireZones property in configuration file if you wish to proceed without any zones or zone provider)");
			System.out.println("No started zone providers found, aborting startup! (disable the /Config/System/RequireZones property in configuration file if you wish to proceed without any zones or zone provider)");
			return;
		}

		List<XMLSettingNode> resolverElements = configFile.getSettings("/Config/Resolvers/Resolver");

		for (XMLSettingNode resolverElement : resolverElements) {

			String name = resolverElement.getString("Name");

			if (StringUtils.isEmpty(name)) {

				log.error("Resolver element with no name set found in config, ignoring element.");
				System.out.println("Resolver element with no name set found in config, ignoring element.");
				continue;
			}

			String className = resolverElement.getString("Class");

			if (StringUtils.isEmpty(className)) {

				log.error("Resolver element " + name + " with no class set found in config, ignoring element.");
				System.out.println("Resolver element " + name + " with no class set found in config, ignoring element.");
				continue;
			}

			try {

				log.debug("Instantiating resolver " + name + " (" + className + ")");

				Resolver resolver = (Resolver) Class.forName(className).newInstance();

				log.debug("Resolver " + name + " successfully instantiated");

				List<XMLSettingNode> propertyElements = resolverElement.getSettings("Properties/Property");

				for (SettingNode propertyElement : propertyElements) {

					String propertyName = propertyElement.getString("@name");

					if (StringUtils.isEmpty(propertyName)) {

						log.error("Property element with no name set found in config for resolver " + name + ", ignoring element");
						System.out.println("Property element with no name set found in config for resolver " + name + ", ignoring element");
						continue;
					}

					String value = propertyElement.getString(".");

					log.debug("Found value " + value + " for property " + propertyName);

					try {
						Method method = resolver.getClass().getMethod("set" + StringUtils.toFirstLetterUppercase(propertyName), String.class);

						ReflectionUtils.fixMethodAccess(method);

						log.debug("Setting property " + propertyName);

						try {

							method.invoke(resolver, value);

						} catch (IllegalArgumentException e) {

							log.error("Unable to set property " + propertyName + " on resolver " + name + " (" + className + ")", e);
							System.out.println("Unable to set property " + propertyName + " on resolver " + name + " (" + className + ")");

						} catch (InvocationTargetException e) {

							log.error("Unable to set property " + propertyName + " on resolver " + name + " (" + className + ")", e);
							System.out.println("Unable to set property " + propertyName + " on resolver " + name + " (" + className + ")");
						}

					} catch (SecurityException e) {

						log.error("Unable to find matching setter method for property " + propertyName + " in resolver " + name + " (" + className + ")", e);
						System.out.println("Unable to find matching setter method for property " + propertyName + " in resolver " + name + " (" + className + ")");

					} catch (NoSuchMethodException e) {

						log.error("Unable to find matching setter method for property " + propertyName + " in resolver " + name + " (" + className + ")", e);
						System.out.println("Unable to find matching setter method for property " + propertyName + " in resolver " + name + " (" + className + ")");
					}
				}

				try {

					resolver.setSystemInterface(this);

					resolver.init(name);

					log.info("Resovler " + name + " (" + className + ") successfully initialized!");
					System.out.println("Resovler " + name + " (" + className + ") successfully initialized!");

					this.resolvers.add(new SimpleEntry<String, Resolver>(name, resolver));

				} catch (Throwable e) {

					log.error("Error initializing resolver " + name + " (" + className + ")", e);
					System.out.println("Error initializing resolver " + name + " (" + className + ")");
				}

			} catch (InstantiationException e) {

				log.error("Unable to create instance of class " + className + " for resolver " + name, e);
				System.out.println("Unable to create instance of class " + className + " for resolver " + name);

			} catch (IllegalAccessException e) {

				log.error("Unable to create instance of class " + className + " for resolver " + name, e);
				System.out.println("Unable to create instance of class " + className + " for resolver " + name);

			} catch (ClassNotFoundException e) {

				log.error("Unable to create instance of class " + className + " for resolver " + name, e);
				System.out.println("Unable to create instance of class " + className + " for resolver " + name);
			}
		}

		if (this.resolvers.isEmpty()) {

			log.fatal("No started resolvers found, aborting startup!");
			System.out.println("No started resolvers found, aborting startup!");
			return;
		}

		List<XMLSettingNode> pluginElements = configFile.getSettings("/Config/Plugins/Plugin");

		for (XMLSettingNode pluginElement : pluginElements) {

			String name = pluginElement.getString("Name");

			if (StringUtils.isEmpty(name)) {

				log.error("Plugin element with no name set found in config, ignoring element.");
				System.out.println("Plugin element with no name set found in config, ignoring element.");
				continue;
			}

			String className = pluginElement.getString("Class");

			if (StringUtils.isEmpty(className)) {

				log.error("Plugin element " + name + " with no class set found in config, ignoring element.");
				System.out.println("Plugin element " + name + " with no class set found in config, ignoring element.");
				continue;
			}

			try {

				log.debug("Instantiating plugin " + name + " (" + className + ")");

				Plugin plugin = (Plugin) Class.forName(className).newInstance();

				log.debug("Plugin " + name + " successfully instantiated");

				List<XMLSettingNode> propertyElements = pluginElement.getSettings("Properties/Property");

				for (SettingNode propertyElement : propertyElements) {

					String propertyName = propertyElement.getString("@name");

					if (StringUtils.isEmpty(propertyName)) {

						log.error("Property element with no name set found in config for plugin " + name + ", ignoring element");
						System.out.println("Property element with no name set found in config for plugin " + name + ", ignoring element");
						continue;
					}

					String value = propertyElement.getString(".");

					log.debug("Found value " + value + " for property " + propertyName);

					try {
						Method method = plugin.getClass().getMethod("set" + StringUtils.toFirstLetterUppercase(propertyName), String.class);

						ReflectionUtils.fixMethodAccess(method);

						log.debug("Setting property " + propertyName);

						try {

							method.invoke(plugin, value);

						} catch (IllegalArgumentException e) {

							log.error("Unable to set property " + propertyName + " on plugin " + name + " (" + className + ")", e);
							System.out.println("Unable to set property " + propertyName + " on plugin " + name + " (" + className + ")");

						} catch (InvocationTargetException e) {

							log.error("Unable to set property " + propertyName + " on plugin " + name + " (" + className + ")", e);
							System.out.println("Unable to set property " + propertyName + " on plugin " + name + " (" + className + ")");
						}

					} catch (SecurityException e) {

						log.error("Unable to find matching setter method for property " + propertyName + " in plugin " + name + " (" + className + ")", e);
						System.out.println("Unable to find matching setter method for property " + propertyName + " in plugin " + name + " (" + className + ")");

					} catch (NoSuchMethodException e) {

						log.error("Unable to find matching setter method for property " + propertyName + " in plugin " + name + " (" + className + ")", e);
						System.out.println("Unable to find matching setter method for property " + propertyName + " in plugin " + name + " (" + className + ")");
					}
				}

				try {

					plugin.setSystemInterface(this);

					plugin.init(name);

					log.info("Plugin " + name + " (" + className + ") successfully initialized!");
					System.out.println("Plugin " + name + " (" + className + ") successfully initialized!");

					this.plugins.put(name, plugin);

				} catch (Throwable e) {

					log.error("Error initializing plugin " + name + " (" + className + ")", e);
					System.out.println("Error initializing plugin " + name + " (" + className + ")");
				}

			} catch (InstantiationException e) {

				log.error("Unable to create instance of class " + className + " for plugin " + name, e);
				System.out.println("Unable to create instance of class " + className + " for plugin " + name);

			} catch (IllegalAccessException e) {

				log.error("Unable to create instance of class " + className + " for plugin " + name, e);
				System.out.println("Unable to create instance of class " + className + " for plugin " + name);

			} catch (ClassNotFoundException e) {

				log.error("Unable to create instance of class " + className + " for plugin " + name, e);
				System.out.println("Unable to create instance of class " + className + " for plugin " + name);
			}
		}
		
		
		log.info("Initializing TCP thread pool...");
		this.tcpThreadPool = new ThreadPoolExecutor(this.tcpThreadPoolSize, this.tcpThreadPoolSize, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

		log.info("Initializing UDP thread pool...");
		this.udpThreadPool = new ThreadPoolExecutor(this.udpThreadPoolSize, this.udpThreadPoolSize, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

		Iterator<InetAddress> iaddr = addresses.iterator();
		while (iaddr.hasNext()) {
			InetAddress addr = iaddr.next();
			Iterator<Integer> iport = ports.iterator();
			while (iport.hasNext()) {
				int port = iport.next().intValue();

				try {
					this.udpMonitorThreads.add(new UDPSocketMonitor(this, addr, port));
				} catch (SocketException e) {
					log.error("Unable to open UDP server socket on address " + addr + ":" + port + ", " + e);
				}

				try {
					this.tcpMonitorThreads.add(new TCPSocketMonitor(this, addr, port));
				} catch (IOException e) {
					log.error("Unable to open TCP server socket on address " + addr + ":" + port + ", " + e);
				}
			}
		}

		if (this.tcpMonitorThreads.isEmpty() && this.udpMonitorThreads.isEmpty()) {

			log.fatal("Not bound on any sockets, aborting startup!");
			System.out.println("Not bound on any sockets, aborting startup!");
			return;
		}

		log.info("Starting secondary zone update timer...");
		this.timerTask = new RunnableTimerTask(this);
		this.secondaryZoneUpdateTimer = new Timer();
		this.secondaryZoneUpdateTimer.schedule(timerTask, MillisecondTimeUnits.SECOND * 60, MillisecondTimeUnits.SECOND * 60);

		log.fatal(VERSION + " started with " + this.primaryZoneMap.size() + " primary zones and " + this.secondaryZoneMap.size() + " secondary zones, " + this.zoneProviders.size() + " Zone providers and " + resolvers.size() + " resolvers");
		System.out.println(VERSION + " started with " + this.primaryZoneMap.size() + " primary zones and " + this.secondaryZoneMap.size() + " secondary zones, " + this.zoneProviders.size() + " Zone providers and " + resolvers.size() + " resolvers");
	}

	public synchronized void shutdown() {

		new Thread() {

			@Override
			public void run() {

				//RMI thread workaround
				actualShutdown();

			}
		}.start();
	}

	synchronized void actualShutdown() {

		if (shutdown == false) {

			log.fatal("Shutting down " + VERSION + "...");
			System.out.println("Shutting down " + VERSION + "...");

			shutdown = true;

			log.info("Stopping secondary zone update timer...");
			timerTask.cancel();
			secondaryZoneUpdateTimer.cancel();

			log.info("Stopping TCP thread pool...");
			tcpThreadPool.shutdown();

			try {
				tcpThreadPool.awaitTermination(tcpThreadPoolShutdownTimeout, TimeUnit.SECONDS);

			} catch (InterruptedException e1) {

				log.error("Timeout waiting " + tcpThreadPoolShutdownTimeout + " seconds for TCP thread pool to shutdown, forcing thread pool shutdown...");
				tcpThreadPool.shutdownNow();
			}

			log.info("Stopping UDP thread pool...");
			udpThreadPool.shutdown();

			try {
				udpThreadPool.awaitTermination(udpThreadPoolShutdownTimeout, TimeUnit.SECONDS);

			} catch (InterruptedException e1) {

				log.error("Timeout waiting " + udpThreadPoolShutdownTimeout + " seconds for UDP thread pool to shutdown, forcing thread pool shutdown...");
				udpThreadPool.shutdownNow();
			}
			
			Iterator<Entry<String,Plugin>> pluginIterator = plugins.entrySet().iterator();
	
			while(pluginIterator.hasNext()){
				
				Entry<String,Plugin> pluginEntry = pluginIterator.next();
				
				stopPlugin(pluginEntry, "plugin");
				
				pluginIterator.remove();
			}
			
			Iterator<Entry<String,Resolver>> resolverIterator = resolvers.iterator();
			
			while(resolverIterator.hasNext()){
				
				Entry<String,Resolver> resolverEntry = resolverIterator.next();
				
				stopPlugin(resolverEntry, "resolver");
				
				resolverIterator.remove();
			}

			Iterator<Entry<String,ZoneProvider>> zoneProviderIterator = zoneProviders.entrySet().iterator();

			while(zoneProviderIterator.hasNext()){
				
				Entry<String,ZoneProvider> zoneProviderEntry = zoneProviderIterator.next();
				
				stopPlugin(zoneProviderEntry, "zone provider");
				
				zoneProviderIterator.remove();
			}			
			
			log.fatal(VERSION + " stopped");
			System.out.println(VERSION + " stopped");

			System.exit(0);
		}
	}
	
	private void stopPlugin(Entry<String,? extends Plugin> pluginEntry, String type){
		
		log.debug("Shutting down " + type + " " + pluginEntry.getKey() + "...");
		
		try{
			pluginEntry.getValue().shutdown();
			
			log.info(type + " " + pluginEntry.getKey() + " shutdown");
			
		}catch(Throwable t){
			
			log.error("Error shutting down " + type + " " + pluginEntry.getKey(), t);
		}
	}
	
	public synchronized void reloadZones() {

		ConcurrentHashMap<Name, CachedPrimaryZone> primaryZoneMap = new ConcurrentHashMap<Name, CachedPrimaryZone>();
		ConcurrentHashMap<Name, CachedSecondaryZone> secondaryZoneMap = new ConcurrentHashMap<Name, CachedSecondaryZone>();

		for (Entry<String, ZoneProvider> zoneProviderEntry : this.zoneProviders.entrySet()) {

			log.info("Getting primary zones from zone provider " + zoneProviderEntry.getKey());

			Collection<Zone> primaryZones;

			try {
				primaryZones = zoneProviderEntry.getValue().getPrimaryZones();

			} catch (Throwable e) {

				log.error("Error getting primary zones from zone provider " + zoneProviderEntry.getKey(), e);
				continue;
			}

			if (primaryZones != null) {

				for (Zone zone : primaryZones) {

					log.info("Got zone " + zone.getOrigin());

					primaryZoneMap.put(zone.getOrigin(), new CachedPrimaryZone(zone, zoneProviderEntry.getValue()));
				}
			}

			log.info("Getting secondary zones from zone provider " + zoneProviderEntry.getKey());

			Collection<SecondaryZone> secondaryZones;

			try {
				secondaryZones = zoneProviderEntry.getValue().getSecondaryZones();

			} catch (Throwable e) {

				log.error("Error getting secondary zones from zone provider " + zoneProviderEntry.getKey(), e);
				continue;
			}

			if (secondaryZones != null) {

				for (SecondaryZone zone : secondaryZones) {

					log.info("Got zone " + zone.getZoneName() + " (" + zone.getRemoteServerAddress() + ")");

					CachedSecondaryZone cachedSecondaryZone = new CachedSecondaryZone(zoneProviderEntry.getValue(), zone);

					secondaryZoneMap.put(cachedSecondaryZone.getSecondaryZone().getZoneName(), cachedSecondaryZone);
				}
			}
		}

		this.primaryZoneMap = primaryZoneMap;
		this.secondaryZoneMap = secondaryZoneMap;
	}

	// @SuppressWarnings("unused")
	// private void addPrimaryZone(String zname, String zonefile) throws IOException {
	// Name origin = null;
	// if (zname != null) {
	// origin = Name.fromString(zname, Name.root);
	// }
	// Zone newzone = new Zone(origin, zonefile);
	// primaryZoneMap.put(newzone.getOrigin(), newzone);
	// }
	//
	// @SuppressWarnings("unused")
	// private void addSecondaryZone(String zone, String remote) throws IOException, ZoneTransferException {
	// Name zname = Name.fromString(zone, Name.root);
	// Zone newzone = new Zone(zname, DClass.IN, remote);
	// primaryZoneMap.put(zname, newzone);
	// }

	@SuppressWarnings("unused")
	private void addTSIG(String algstr, String namestr, String key) throws IOException {

		Name name = Name.fromString(namestr, Name.root);
		TSIGs.put(name, new TSIG(algstr, namestr, key));
	}

	public Zone getZone(Name name) {

		CachedPrimaryZone cachedPrimaryZone = this.primaryZoneMap.get(name);

		if (cachedPrimaryZone != null) {
			return cachedPrimaryZone.getZone();
		}

		CachedSecondaryZone cachedSecondaryZone = this.secondaryZoneMap.get(name);

		if (cachedSecondaryZone != null && cachedSecondaryZone.getSecondaryZone().getZoneCopy() != null) {

			return cachedSecondaryZone.getSecondaryZone().getZoneCopy();
		}

		return null;
	}

	/*
	 * Note: a null return value means that the caller doesn't need to do anything. Currently this only happens if this is an AXFR request over TCP.
	 */
	byte[] generateReply(Message query, byte[] in, int length, Socket socket, SocketAddress socketAddress) throws IOException {

		if (log.isDebugEnabled()) {

			log.debug("Processing query " + toString(query.getQuestion()) + " from " + socketAddress);
			log.debug("Full query:\n" + query);
		}

		Message response = null;

		Request request = new SimpleRequest(socketAddress, query, in, length, socket);

		for (Entry<String, Resolver> resolverEntry : resolvers) {

			try {
				response = resolverEntry.getValue().generateReply(request);

				if (response != null) {

					log.info("Got response from resolver " + resolverEntry.getKey() + " for query " + toString(query.getQuestion()));

					if (log.isDebugEnabled()) {

						log.debug(response);
					}

					break;
				}

			} catch (Exception e) {

				log.error("Caught exception from resolver " + resolverEntry.getKey(), e);
			}

		}

		if (socket != null && socket.isClosed()) {

			//Response already comitted;
			return null;
		}

		OPTRecord queryOPT = query.getOPT();

		if (response == null) {

			response = getInternalResponse(query, in, length, socket, queryOPT);

			log.info("Got no response from resolvers for query " + toString(query.getQuestion()) + " sending default response " + Rcode.string(this.defaultResponse));
		}

		int maxLength;

		if (socket != null) {
			maxLength = 65535;
		} else if (queryOPT != null) {
			maxLength = Math.max(queryOPT.getPayloadSize(), 512);
		} else {
			maxLength = 512;
		}

		return response.toWire(maxLength);
	}

	private Message getInternalResponse(Message query, byte[] in, int length, Socket socket, OPTRecord queryOPT) {

		Header header;
		// boolean badversion;
		int flags = 0;

		header = query.getHeader();
		if (header.getFlag(Flags.QR)) {
			return null;
		}
		if (header.getRcode() != Rcode.NOERROR) {
			return errorMessage(query, Rcode.FORMERR);
		}
		if (header.getOpcode() != Opcode.QUERY) {
			return errorMessage(query, Rcode.NOTIMP);
		}

		TSIGRecord queryTSIG = query.getTSIG();
		TSIG tsig = null;
		if (queryTSIG != null) {
			tsig = TSIGs.get(queryTSIG.getName());
			if (tsig == null || tsig.verify(query, in, length, null) != Rcode.NOERROR) {
				return formerrMessage(in);
			}
		}

		//		if (queryOPT != null && queryOPT.getVersion() > 0) {
		//			// badversion = true;
		//		}

		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0) {
			flags = FLAG_DNSSECOK;
		}

		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		if (query.getHeader().getFlag(Flags.RD)) {
			response.getHeader().setFlag(Flags.RD);
		}

		response.getHeader().setRcode(defaultResponse);

		Record queryRecord = query.getQuestion();

		response.addRecord(queryRecord, Section.QUESTION);

		int type = queryRecord.getType();

		if (type == Type.AXFR && socket != null) {
			return errorMessage(query, Rcode.REFUSED);
		}
		if (!Type.isRR(type) && type != Type.ANY) {
			return errorMessage(query, Rcode.NOTIMP);
		}

		if (queryOPT != null) {
			int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
			OPTRecord opt = new OPTRecord((short) 4096, defaultResponse, (byte) 0, optflags);
			response.addRecord(opt, Section.ADDITIONAL);
		}

		response.setTSIG(tsig, defaultResponse, queryTSIG);

		return response;
	}

	public static Message buildErrorMessage(Header header, int rcode, Record question) {

		Message response = new Message();

		response.setHeader(header);
		for (int i = 0; i < 4; i++) {
			response.removeAllRecords(i);
		}
		if (rcode == Rcode.SERVFAIL) {
			response.addRecord(question, Section.QUESTION);
		}
		header.setRcode(rcode);
		return response;
	}

	Message formerrMessage(byte[] in) {

		Header header;
		try {
			header = new Header(in);
		} catch (IOException e) {
			return null;
		}
		return buildErrorMessage(header, Rcode.FORMERR, null);
	}

	public static Message errorMessage(Message query, int rcode) {

		return buildErrorMessage(query.getHeader(), rcode, query.getQuestion());
	}

	protected void UDPClient(DatagramSocket socket, DatagramPacket inDataPacket) {

	}

	public static String toString(Record record) {

		if (record == null) {

			return null;
		}

		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(record.getName());

		stringBuilder.append(" ");

		stringBuilder.append(record.getTTL());

		stringBuilder.append(" ");

		stringBuilder.append(DClass.string(record.getDClass()));

		stringBuilder.append(" ");

		stringBuilder.append(Type.string(record.getType()));

		String rdata = record.rdataToString();

		if (!rdata.equals("")) {
			stringBuilder.append(" ");
			stringBuilder.append(rdata);
		}

		return stringBuilder.toString();
	}

	public static void main(String[] args) {

		if (args.length > 1) {
			System.out.println("usage: EagleDNS [conf]");
			System.exit(0);
		}

		try {
			String configFilePath;

			if (args.length == 1) {

				configFilePath = args[0];

			} else {

				configFilePath = "conf/config.xml";
			}

			new EagleDNS(configFilePath);

		} catch (IOException e) {

			System.out.println(e);
		}
	}

	public void run() {

		log.debug("Checking secondary zones...");

		for (CachedSecondaryZone cachedSecondaryZone : this.secondaryZoneMap.values()) {

			SecondaryZone secondaryZone = cachedSecondaryZone.getSecondaryZone();

			if (secondaryZone.getZoneCopy() == null || secondaryZone.getDownloaded() == null || (System.currentTimeMillis() - secondaryZone.getDownloaded().getTime()) > (secondaryZone.getZoneCopy().getSOA().getRefresh() * 1000)) {

				cachedSecondaryZone.update(this.axfrTimeout);
			}
		}
	}

	protected ThreadPoolExecutor getTcpThreadPool() {

		return tcpThreadPool;
	}

	protected ThreadPoolExecutor getUdpThreadPool() {

		return udpThreadPool;
	}

	public boolean isShutdown() {

		return shutdown;
	}

	public TSIG getTSIG(Name name) {

		return this.TSIGs.get(name);
	}

	public int primaryZoneCount() {

		return primaryZoneMap.size();
	}

	public int secondaryZoneCount() {

		return secondaryZoneMap.size();
	}

	public int getResolverCount() {

		return resolvers.size();
	}

	public int getActiveTCPThreadCount() {

		return tcpThreadPool.getActiveCount();
	}

	public int getTCPThreadPoolSize() {

		return tcpThreadPool.getCorePoolSize();
	}

	public long getCompletedTCPQueryCount() {

		return tcpThreadPool.getCompletedTaskCount();
	}

	public long getTCPQueueSize() {

		return tcpThreadPool.getQueue().size();
	}

	public int getMaxActiveTCPThreadCount() {

		return this.tcpThreadPool.getLargestPoolSize();
	}

	public int getActiveUDPThreadCount() {

		return udpThreadPool.getActiveCount();
	}

	public int getUDPThreadPoolSize() {

		return udpThreadPool.getCorePoolSize();
	}

	public long getCompletedUDPQueryCount() {

		return udpThreadPool.getCompletedTaskCount();
	}

	public long getUDPQueueSize() {

		return udpThreadPool.getQueue().size();
	}

	public int getMaxActiveUDPThreadCount() {

		return this.udpThreadPool.getLargestPoolSize();
	}

	public long getStartTime() {

		return this.startTime;
	}

	public String getVersion() {

		return VERSION;
	}

	public Plugin getPlugin(String name) {

		// TODO Auto-generated method stub
		return null;
	}

	public Set<Entry<String, Plugin>> getPlugins() {

		// TODO Auto-generated method stub
		return null;
	}

	public Resolver getResolver(String name) {

		// TODO Auto-generated method stub
		return null;
	}

	public List<Entry<String, Resolver>> getResolvers() {

		// TODO Auto-generated method stub
		return null;
	}

	public ZoneProvider getZoneProvider(String name) {

		// TODO Auto-generated method stub
		return null;
	}

	public Set<Entry<String, ZoneProvider>> getZoneProviders() {

		// TODO Auto-generated method stub
		return null;
	}
}
