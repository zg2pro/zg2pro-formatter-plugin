package aptea.spring;

import irsn.Settings;

import java.util.Properties;

/**
 * User: samuel.herve
 * Date: 7/17/12
 */
public class SigisJNDIEnvironment extends Properties {
	public SigisJNDIEnvironment() {
		super();
		this.put("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
		this.put("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
		this.put("java.naming.factory.state", "com.sun.corba.ee.impl.presentation.rmi.JNDIStateFactoryImpl");

		// Override get() pour avoir les paramètres en dynamique.
		this.put("org.omg.CORBA.ORBInitialHost", Settings.SERVICE_JNDI_HOST.getSetting());
		this.put("org.omg.CORBA.ORBInitialPort", Settings.SERVICE_JNDI_PORT.getSetting());
		this.put("org.omg.CORBA.ORBTCPTimeouts", Settings.SERVICE_JNDI_TIMEOUT.getSetting());
		this.put("java.naming.provider.url","iiop://" + Settings.SERVICE_JNDI_HOST.getSetting() + ":" + Settings.SERVICE_JNDI_PORT.getSetting());
	}
}
