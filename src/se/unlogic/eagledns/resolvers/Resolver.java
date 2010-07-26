/*******************************************************************************
 * Copyright (c) 2010 Robert "Unlogic" Olofsson (unlogic@unlogic.se).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ******************************************************************************/
package se.unlogic.eagledns.resolvers;

import java.io.IOException;

import org.xbill.DNS.Message;

import se.unlogic.eagledns.Request;
import se.unlogic.eagledns.plugins.Plugin;

/**
 * An extension of the {@link Plugin} interface used to create resolvers for Eagle DNS.
 * 
 * @author Robert "Unlogic" Olofsson
 *
 */
public interface Resolver extends Plugin{
	
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
}
