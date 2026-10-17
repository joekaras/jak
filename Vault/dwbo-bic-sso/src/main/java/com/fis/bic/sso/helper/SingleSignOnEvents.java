package com.fis.bic.sso.helper;


import com.fis.bic.sso.utility.BicLogger;
import com.fis.bic.sso.utility.BicssoConstants;
import com.fis.bic.sso.utility.EncryptedProperties;
import com.fis.bic.sso.utility.ILogin;

public final  class SingleSignOnEvents implements  BicssoConstants{
	 
	private static SingleSignOnEvents ssoeventref;
	private static BicLogger logger = BicLogger.getLogger(SingleSignOnEvents.class);
	public EncryptedProperties encprop;	
	private ILogin login;

	private SingleSignOnEvents() {
		encprop = EncryptedProperties.getSingletonObject(PROPERTY_LOOKUP_FILENAME);
	}

	public static SingleSignOnEvents getSingletonObject(){
		if (ssoeventref == null) {
			logger.warn("Created new instatnce of the SingleSignOnEvents singleton instance. Should only happen servlet startup.",SingleSignOnEvents.class);
			ssoeventref = new SingleSignOnEvents();
			
		}
		return ssoeventref;

	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public ILogin getLogin() {
		return login;		
	}

	public void setLogin(final ILogin ilogin) {
		login = ilogin;
	}

}
