package se.unlogic.eagledns.resolvers;

import org.xbill.DNS.Message;

import se.unlogic.eagledns.Request;
import se.unlogic.eagledns.Resolver;
import se.unlogic.eagledns.SystemInterface;


/**
 * This resolver is strictly authorative and uses Eagle DNS internal cache of authorative zones to answer queries.
 * 
 * @author Robert "Unlogic" Olofsson (unlogic@unlogic.se)
 *
 */
public class AuthorativeResolver implements Resolver {

	public Message generateReply(Request request) throws Exception {

		// TODO Auto-generated method stub
		return null;
	}

	public byte getPriority() {

		// TODO Auto-generated method stub
		return 0;
	}

	public void setSystemInterface(SystemInterface systemInterface) {

		// TODO Auto-generated method stub

	}

}
