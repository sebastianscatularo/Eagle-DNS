package se.unlogic.eagledns.resolvers;

import org.apache.log4j.Logger;

import se.unlogic.eagledns.Resolver;
import se.unlogic.eagledns.SystemInterface;


public abstract class BaseResolver implements Resolver {

	protected Logger log = Logger.getLogger(this.getClass());
	
	protected SystemInterface systemInterface;
	
	protected String name;
	
	public void setSystemInterface(SystemInterface systemInterface) {

		this.systemInterface = systemInterface;
	}

	public void init(String name) throws Exception{
		
		this.name = name;
	}
}
