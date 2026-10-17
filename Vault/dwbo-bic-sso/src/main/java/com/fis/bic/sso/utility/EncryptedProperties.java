package com.fis.bic.sso.utility;

import java.io.*;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;



/*
 * uses Java Data Protection API 
 * http://jdpapi.sourceforge.net/
 * 
 */
public class EncryptedProperties {

	//private static DataProtector protector;
	private static Properties properties;
	//private static Base64 base64;
	//private static Map<String, String> keys;
	private static Map<String, String> filecached = new HashMap<String, String>();
	private static EncryptedProperties propkeyref;
	private static BicLogger logger = BicLogger.getLogger(EncryptedProperties.class);


	public EncryptedProperties() {
		//EncryptedProperties.protector = new DataProtector();
		EncryptedProperties.properties = new Properties();
		//EncryptedProperties.base64 = new Base64();
		//EncryptedProperties.keys = new HashMap<String, String>();
	}

	/*
	 * public EncryptedProperties(String filename) { this.protector = new
	 * DataProtector(); this.properties = new Properties(); this.base64 = new
	 * Base64(); this.keys = new HashMap<String, String>(); this.filename =
	 * filename; }
	 */

	public static EncryptedProperties getSingletonObject(final String filename) {
		
		if (propkeyref == null){
			propkeyref = new EncryptedProperties();
		}
		if(!(filecached.get(filename) == filename )){
			filecached.put(filename, filename);		
			EncryptedProperties.load(filename);
		}
		return propkeyref;

	}

	public void store(final OutputStream writer) throws IOException {
		properties.store(writer, "");
	}

	public static boolean load(final String filename) {
		try {
			properties.load(EncryptedProperties.class.getClassLoader() // NOPMD by e3002373 on 7/10/13 12:21 PM
					.getResourceAsStream(filename));
		} catch (IOException e) {
			logger.error("Application Property File load failed :",e);
		}
		Enumeration<?> eProps = properties.propertyNames();
		while (eProps.hasMoreElements()) {
			String key = (String) eProps.nextElement();
			String value = properties.getProperty(key);
			//String encryptedKey = encrypt(key);
			//keys.put(key, encryptedKey);		        
		    //properties.setProperty(encryptedKey, value);
			
			properties.setProperty(key, value);
		}
		return true;
	}

	public static void setProperty(final String key, final String value) {
		//String encryptedKey = encrypt(key);
		//String encryptedValue = encrypt(value);
		//keys.put(key, encryptedKey);

		//properties.setProperty(encryptedKey, encryptedValue);
		properties.setProperty(key, value);
	}

	public String getProperty(final String key) {
		//String encryptedKey = keys.get(key);
		//String encryptedValue = properties.getProperty(encryptedKey);
		//return decrypt(encryptedValue);
		
		String value = properties.getProperty(key);
		return value;
	}
/*
	private static String encrypt(final String key) {
		byte[] data = protector.protect(key);
		return new String(base64.encode(data));
	}

	private String decrypt(final String encryptedString) {
		byte[] data = base64.decode(encryptedString.getBytes());
		return protector.unprotect(data);
	}
*/
	/*
	static {
		try{
			System.out.println("loading again from bic jar");
			//System.loadLibrary("jdpapi-native-1.0.1.dll");
			//System.load("jdpapi-native-1.0.1.dll");
			System.load(new File("c:\\temp\\Deployment\\SSOInitialSetup\\64Bit\\jdpapi-native-1.0.1.dll").getAbsolutePath());
		   //System.load(new File("c:jdpapi-native-1.0.1.dll").getAbsolutePath());
		}catch(Exception ee){
			System.out.println("loading again error from bic jar"+ee.getLocalizedMessage());
		}
	}
	*/
	
	
}