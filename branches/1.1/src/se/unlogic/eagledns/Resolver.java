package se.unlogic.eagledns;

import java.io.IOException;

import org.xbill.DNS.Message;

public interface Resolver {

	/**
	 * This method is called when the resolver is requested to process a query.
	 * <p>
	 * 
	 * If the resolver returns null and the socket is still open Eagle DNS will pass the query to the next resolver. Although it should be noted that a socket
	 * is only passed on the resolver in case of TCP queries for UDP queries the socket criteria is ignored.
	 * 
	 * @param request
	 *            the incoming query
	 * @return
	 * @throws IOException
	 */
	public Message generateReply(Request request) throws Exception;

	/**
	 * This method should return the priority of the resolver. The priority is used by Eagle DNS to determine in which order the resolvers should be called when
	 * processing a query.
	 * 
	 * @return the priority of this resolver
	 */
	public byte getPriority();

	/**
	 * @param systemInterface
	 *            that the resolver can use to access the internal functions of Eagle DNS
	 */
	public void setSystemInterface(SystemInterface systemInterface);
}
