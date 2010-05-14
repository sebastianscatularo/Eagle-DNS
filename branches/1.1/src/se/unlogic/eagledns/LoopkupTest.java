package se.unlogic.eagledns;

import java.net.UnknownHostException;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;

import se.unlogic.standardutils.string.StringUtils;


public class LoopkupTest {

	/**
	 * @param args
	 * @throws TextParseException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws TextParseException, UnknownHostException {

		String domain = "unlogic.se";
		
		Lookup lookup1 = new Lookup(domain);
		
		Lookup lookup2 = new Lookup(domain);
		
		SimpleResolver resolver = new SimpleResolver("localhost");
		
		lookup2.setResolver(resolver);
				
		lookup2.run();
		lookup1.run();
		
		
		System.out.println("1: " + StringUtils.toCommaSeparatedString(lookup1.getAnswers()));
		System.out.println("2: " + StringUtils.toCommaSeparatedString(lookup2.getAnswers()));
	}

}
