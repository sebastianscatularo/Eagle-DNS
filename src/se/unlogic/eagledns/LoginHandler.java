package se.unlogic.eagledns;

import org.apache.log4j.Logger;


public class LoginHandler implements EagleLogin {

	private Logger log = Logger.getLogger(this.getClass());

	private EagleManager eagleManager;
	private String password;

	public LoginHandler(EagleManager eagleManager, String password) {
		super();
		this.eagleManager = eagleManager;
		this.password = password;
	}

	public EagleManager login(String password) {

		if(password != null && password.equalsIgnoreCase(this.password)){

			log.info("Remote login");

			return eagleManager;

		}

		log.warn("Failed login attempt");

		return null;
	}

}
