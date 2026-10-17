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

package com.fis.bic.sso.utility;

import java.util.Hashtable;

public class LoginRequest 
{
	
	public String Address;
	public String LoginToken;

	public Hashtable<String, String> Parameters = new Hashtable<String, String>();
}

