/*
 * ===================================================================
 * Copyright Notice! 
 * ------------------------------------------------------------------- 
 * This document is protected under the trade secret and copyright 
 * laws as the property of Fidelity National Information Services, Inc. 
 * Copying, reproduction or distribution should be limited and only to
 * employees with a "need to know" to do their job. 
 * Any disclosure of this document to third parties 
 * is strictly prohibited.
 *
 * copyright 2015 Fidelity National Information Services
 * All rights reserved worldwide.
 * ===================================================================
 */
package com.fis.businessobjects.helper;

import com.crystaldecisions.sdk.framework.*;
import com.crystaldecisions.sdk.exception.*;

/**
 * @author e1032994
 * Business objects helper class
 */
public class SessionManager 
{

	/*
	 * Logins into Business Objects session using Single Sign On parameters.
	 */
	public static IEnterpriseSession LoginSSO(String userName, String cms, String secretKey) throws SDKException
	{
		ISessionMgr sm = com.crystaldecisions.sdk.framework.CrystalEnterprise.getSessionMgr();
	    
		ITrustedPrincipal tp = sm.createTrustedPrincipal(userName, cms, secretKey);
		IEnterpriseSession es = sm.logon(tp);
		
		return es;
	}
}
