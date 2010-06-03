package se.unlogic.eagledns.plugins;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import se.unlogic.eagledns.EagleLogin;
import se.unlogic.eagledns.EagleManager;
import se.unlogic.eagledns.LoginHandler;


public class RMIRemoteManagementPlugin extends BasePlugin{

	private String remotePassword;
	private Integer remotePort;
	private LoginHandler loginHandler;
	
	@Override
	public void init(String name) throws Exception {

		super.init(name);
		
		if (remotePassword == null || remotePort == null) {

			throw new RuntimeException("Remote managed port and/or password not set, unable to start RMI remote managent plugin.");

		} else {

			log.info("Plugin " + this.name + " starting RMI remote management interface on port " + remotePort);

			EagleManager eagleManager = new SystemInterfaceWrapper(systemInterface);
			
			this.loginHandler = new LoginHandler(eagleManager, this.remotePassword);

			try {
				EagleLogin eagleLogin = (EagleLogin) UnicastRemoteObject.exportObject(loginHandler, remotePort);
				UnicastRemoteObject.exportObject(eagleManager, remotePort);

				Registry registry = LocateRegistry.createRegistry(remotePort);

				registry.bind("eagleLogin", eagleLogin);

			} catch (AccessException e) {

				throw e;

			} catch (RemoteException e) {

				throw e;

			} catch (AlreadyBoundException e) {

				throw e;
			}
		}		
	}
	
	public void setRemotePassword(String remotePassword) {
	
		this.remotePassword = remotePassword;
	}

	
	public void setRemotePort(Integer remotePort) {
	
		this.remotePort = remotePort;
	}
}
