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
 * copyright 2024 Fidelity National Information Services
 * All rights reserved worldwide.
 * ===================================================================
 */

package com.fis.businessobjects.helper;

import com.fis.bic.sso.utility.BicLogger;
import com.crystaldecisions.sdk.framework.*;
import com.crystaldecisions.sdk.plugin.desktop.user.*;
import com.fis.businessobjects.helper.SessionManager;
import com.crystaldecisions.sdk.occa.infostore.*;
import com.crystaldecisions.sdk.plugin.desktop.server.IServer;
import com.crystaldecisions.sdk.exception.*;

import java.util.*;
import java.io.IOException;
import java.util.Random;
import java.security.SecureRandom;

/**
 * @author e1032994
 * Password Management class
 */
public class PasswordManagement 
{
    // Strings used to create a new random password. 
    private static final String Charset = "!@#$%^&*()0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String Charsetspecial = "!@#$%^&*()";
    private static final String Charsetnum = "0123456789";
    
    private static BicLogger logger = BicLogger.getLogger(PasswordManagement.class);
    	
    public static Boolean SetPasswordForChangeOnNextLogin(String adminName, String userName, String cms, String boSecretKey, int LengthOfStringDesired)
    {
    	
    	IEnterpriseSession boEnterpriseSession = null;
    	
    	try 
    	{
			boEnterpriseSession = SessionManager.LoginSSO(adminName, cms, boSecretKey);
			
			// check to see if user needs to change password on next login
			IInfoStore iStore = (IInfoStore) boEnterpriseSession.getService("InfoStore"); 

			IInfoObjects users = iStore.query("Select SI_FORCE_PASSWORD_CHANGE From CI_SYSTEMOBJECTS Where SI_PROGID='CrystalEnterprise.USER' and " + "SI_NAME ='" + userName + "' " ); 
			
			if(users.size() > 0)
			{ 
				IInfoObject infoObject = (IInfoObject)users.get(0); 
				IUser user = (IUser)infoObject; 
				
				// if yes, generate a random password for user and set it
				if (user.isPasswordToChangeAtNextLogon())
				{
					String newCred = PasswordManagement.GenerateRandomPassword(LengthOfStringDesired);
				
					if (PasswordManagement.SetPassword(adminName, userName, newCred, cms, boSecretKey) == false)
					{
						return false;
					}
				}
			}
			else
			{
				return false;
			}
		}
		catch (Exception e) 
		{
			logger.error(e.getMessage(),e);
			return false;
		}
    	finally
    	{
    		if (boEnterpriseSession!=null)
    		{
    			boEnterpriseSession.logoff();
    		}
    	}

		return true;
    	
    }
    
    
    public static Boolean SetPassword(String adminId, String userName, String newCred, String cms, String boSecretKey)
    {              
    	BicLogger log = BicLogger.getLogger(PasswordManagement.class);
                    
    	try
    	{               
    		//get the number of times to try to run through the cms's
    		int updatecmsloops = 3;
                                    
    		IEnterpriseSession enterpriseSession = SessionManager.LoginSSO(adminId, cms, boSecretKey);
                                    
    		//get all the servers in the cluster
    		String[] clustermemebers=enterpriseSession.getClusterMembers();

    		IInfoStore iStore = (IInfoStore) enterpriseSession.getService("InfoStore");  
                                    
    		//get active cms's
    		ArrayList<String> activecms=new ArrayList<String>();
    		IInfoObjects CMSs = iStore.query("select SI_SERVER_DESCRIPTOR, SI_NAME, SI_SERVER_IS_ALIVE, SI_DISABLED, SI_SERVER_NAME, SI_SIA_HOSTNAME From CI_SYSTEMOBJECTS Where SI_KIND='Server' and SI_SERVER_DESCRIPTOR='aps' and SI_SERVER_IS_ALIVE=1");
    		for (int i=0; i<CMSs.size(); i++)
    		{
    			IInfoObject obj = (com.crystaldecisions.sdk.occa.infostore.IInfoObject)CMSs.get(i);
    			IServer server=(IServer)obj;
    			activecms.add(server.getServerName());
    		}
    		enterpriseSession.logoff();
                                    
    		ArrayList<String> foundcms=new ArrayList<String>();
                                    
    		while (updatecmsloops > 0)
    		{
    			for  (String member : clustermemebers)
    			{
    				enterpriseSession = SessionManager.LoginSSO(adminId, member, boSecretKey);
    				if (foundcms.contains(enterpriseSession.getCMSName()) == false)
    				{
    					iStore = (IInfoStore) enterpriseSession.getService("InfoStore");
    					IInfoObjects users = iStore.query("Select SI_PASSWORD, SI_FORCE_PASSWORD_CHANGE From CI_SYSTEMOBJECTS Where SI_PROGID='CrystalEnterprise.USER' and " + "SI_NAME ='" + userName + "' " );
    					if(users.size() > 0)
    					{ 
    						IInfoObject infoObject = (IInfoObject)users.get(0); 
    						IUser user = (IUser)infoObject; 
                                                                                                    
    						user.setNewPassword(newCred);
    						user.setPasswordToChangeAtNextLogon(false);
                                                                                    
    						iStore.commit(users);
    						foundcms.add(enterpriseSession.getCMSName());
    						log.info("Password set successfully for user: " + userName + " on cms: " + member);
    					}
    					else
    					{
    						log.warn("Failed to find user in Business Objects InfoStore.  User: " + userName + "on cms: " + member);       
    					}
    				}
    				enterpriseSession.logoff();
                                                                    
    				//check to see if all of the cms's have been hit with the new password
                                                                    
    				if (foundcms.size() == activecms.size())
    				{
    					//break out of the cluster member loop because all cms's have been hit
    					return true;
    				}
    			}                                     
    			updatecmsloops--;
    		}
                                    
    		//didn't get to update all of the cms's - sad
    		log.error("Failed to update password on all active cms's for user: " + userName);
                                    
    	}
    	catch (SDKException ex)
    	{
    		log.error("exception: " + ex.getCause() + " " + ex.getMessage());
    		return false;
    	}
    	
    	/*catch (IOException iEx)
    	{
    		log.error("exception: " + iEx.getCause() + " " + iEx.getMessage());
    		return false;
    	}
        */          
    	return false;
    }

    public static Boolean SetBO4Password(String adminId, String userName, String newCred, String cms4, String bo4SecretKey)
    {              
    	BicLogger log = BicLogger.getLogger(PasswordManagement.class);
                    
    	try
    	{               
    		//get the number of times to try to run through the cms's
    		int updatecmsloops = 3;
                                    
    		IEnterpriseSession enterpriseSession = SessionManager.LoginSSO(adminId, cms4, bo4SecretKey);
                                    
    		//get all fo the servers in the cluster
    		String[] clustermemebers=enterpriseSession.getClusterMembers();

    		IInfoStore iStore = (IInfoStore) enterpriseSession.getService("InfoStore");  
                                    
    		//get active cms's
    		ArrayList<String> activecms=new ArrayList<String>();
    		IInfoObjects CMSs = iStore.query("select SI_SERVER_DESCRIPTOR, SI_NAME, SI_SERVER_IS_ALIVE, SI_DISABLED, SI_SERVER_NAME, SI_SIA_HOSTNAME From CI_SYSTEMOBJECTS Where SI_KIND='Server' and SI_SERVER_DESCRIPTOR='aps' and SI_SERVER_IS_ALIVE=1");
    		for (int i=0; i<CMSs.size(); i++)
    		{
    			IInfoObject obj = (com.crystaldecisions.sdk.occa.infostore.IInfoObject)CMSs.get(i);
    			IServer server=(IServer)obj;
    			activecms.add(server.getServerName());
    		}
    		enterpriseSession.logoff();
                                    
    		ArrayList<String> foundcms=new ArrayList<String>();
                                    
    		while (updatecmsloops > 0)
    		{
    			for  (String member : clustermemebers)
    			{
    				enterpriseSession = SessionManager.LoginSSO(adminId, member, bo4SecretKey);
    				if (foundcms.contains(enterpriseSession.getCMSName()) == false)
    				{
    					iStore = (IInfoStore) enterpriseSession.getService("InfoStore");
    					IInfoObjects users = iStore.query("Select SI_NEW_PASSWORD, SI_FORCE_PASSWORD_CHANGE From CI_SYSTEMOBJECTS Where SI_PROGID='CrystalEnterprise.USER' and " + "SI_NAME ='" + userName + "' " );
    					if(users.size() > 0)
    					{ 
    						IInfoObject infoObject = (IInfoObject)users.get(0); 
    						IUser user = (IUser)infoObject; 
                                                                                                    
    						user.setNewPassword(newCred);
    						user.setPasswordToChangeAtNextLogon(false);
                                                                                    
    						iStore.commit(users);
    						foundcms.add(enterpriseSession.getCMSName());
    					}
    					else
    					{
    						log.warn("Failed to find user in Business Objects InfoStore.  User: " + userName + "on cms: " + member);       
    					}
    				}
    				enterpriseSession.logoff();
                                                                    
    				//check to see if all of the cms's have been hit with the new password
                                                                    
    				if (foundcms.size() == activecms.size())
    				{
    					//break out of the cluster member loop because all cms's have been hit
    					return true;
    				}
    			}                                     
    			updatecmsloops--;
    		}
                                    
    		//didn't get to update all of the cms's - sad
    		log.error("Failed to update password on all active cms's for user: " + userName);
                                    
    	}
    	catch (SDKException ex)
    	{
    		log.error("exception: " + ex.getCause() + " " + ex.getMessage());
    		return false;
    	}
    	
    	/*catch (IOException iEx)
    	{
    		log.error("exception: " + iEx.getCause() + " " + iEx.getMessage());
    		return false;
    	}
        */          
    	return false;
    }

    /// <summary>
    ///  Generates random password.
    /// </summary>
    /// <param name="length">Length of password.</param>
    /// <returns>Generated password.</returns>
    public static String GenerateRandomPassword(int length)
    {
    	SecureRandom rand = new SecureRandom();
    	SecureRandom randomGenerator = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        int randomspec = randomGenerator.nextInt(length);
        int randomnum = randomGenerator.nextInt(length);
        for (int i = 0; i < length; i++)
        {
            if (i == randomspec)
            {
                int pos = rand.nextInt(Charsetspecial.length());
                sb.append(Charsetspecial.charAt(pos));
            }
            else if (i == randomnum)
            {
                int pos = rand.nextInt(Charsetnum.length());
                sb.append(Charsetnum.charAt(pos));
            }
            else
            {
                int pos = rand.nextInt(Charset.length());
                sb.append(Charset.charAt(pos));
            }
        }
        return sb.toString();
    }
}
