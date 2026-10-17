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
package com.fis.bic.sso.helper;

import java.util.Iterator;
//import java.util.Hashtable;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.internal.InfoStore;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAlias;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAliases;
import com.crystaldecisions.sdk.occa.security.ILogonTokenMgr;
import com.fis.bic.sso.helper.SecretKeyStorage;
import com.fis.bic.sso.helper.SingleSignOnConfiguration;
import com.fis.bic.sso.utility.BicSsoException;
import com.fis.bic.sso.utility.ILogin;
import com.fis.bic.sso.utility.LoginRequest;
import com.fis.bic.sso.utility.BicLogger;
import com.fis.businessobjects.helper.PasswordManagement;
import com.fis.businessobjects.helper.SessionManager;

public class LoginHelper implements ILogin
{
	private static BicLogger logger = BicLogger.getLogger(LoginHelper.class);
	
	public LoginRequest TokenLoginBO4(String userName) throws BicSsoException 
	{
		logger.debug(userName);
		
		SecretKeyStorage seckeyObj = SecretKeyStorage.getSingletonObject();
		String bo4SecretKey = seckeyObj.readConfiguredBO4SecretKey();
		
		String token = null;
		
		IEnterpriseSession boEnterpriseSession=null;

		try 
		{		 
			SingleSignOnConfiguration configuration = SingleSignOnConfiguration.getSingletonObject();
			
			// Set the user password, if user needs to change password on next login
			if (false == PasswordManagement.SetPasswordForChangeOnNextLogin(configuration.getAdministratorBO4UserName()
															, userName
															, configuration.getCMS4Name()
															, bo4SecretKey
															, seckeyObj.readConfiguredMinimumPasswordLength()))
			{
				logger.error("Failed to set password for change password on next login for user: ", userName);
				return null;
			}
			boEnterpriseSession = SessionManager.LoginSSO(userName, configuration.getCMS4Name(), bo4SecretKey);
			ILogonTokenMgr tm = boEnterpriseSession.getLogonTokenMgr();

			token = tm.createLogonToken("", 1, 1);

			if (token != null) 
			{
				logger.info("redirect into BO using token for account:", userName);

				LoginRequest request = new LoginRequest();

				request.LoginToken = token;
				return request;
			}
			else 
			{
				logger.error("Token Generated is invalid for account :", userName);
				return null;
			}
		} 
		catch (SDKException sdke) 
		{
			if(sdke.getDetailMessage().contains("disabled"))
			{
				// Mythos remediation: Legacy Vulnerability ID a6640101-62aa-432c-b283-f7c87c04cc43; SNOW AVIT32772774
				// Fail closed: accounts are disabled deliberately (dormancy
				// maintenance or admin action). Never auto-re-enable and
				// retry from the SSO path.
				logger.error("Login rejected, account is disabled:", userName);
				return null;
			} 
			else
			{	
				logger.error("Token creation failed:"+ userName,sdke);
        		return null;		
        	}		
		} 
		catch (Exception e) 
		{
			logger.error("Exception: "+e.getMessage());
			return null;
		}
		finally
		{
			if (boEnterpriseSession!=null)
			{
				boEnterpriseSession.logoff();
			}
		}
	}
	
	public boolean unlockUser(String userName) throws BicSsoException 
	{
		boolean unlock = false;
		IEnterpriseSession boEnterpriseSession=null;
		try 
		{
			SingleSignOnConfiguration ssoConfigObj = SingleSignOnConfiguration.getSingletonObject();
			SecretKeyStorage seckeyObj = SecretKeyStorage.getSingletonObject();
			String adminName = ssoConfigObj.getAdministratorBO4UserName();
			String boSecretKey = seckeyObj.readConfiguredSecretKey();
			
			boEnterpriseSession = SessionManager.LoginSSO(adminName, ssoConfigObj.getCMS4Name(), boSecretKey);

			InfoStore iStore = (InfoStore) boEnterpriseSession.getService("InfoStore");
			IInfoObjects users = iStore
					.query("Select SI_PASSWORD, SI_ALIASES from CI_SYSTEMOBJECTS WHERE SI_KIND='User' and "
							+ "SI_NAME ='" + userName + "' ");

			if (users.size() > 0) 
			{
				IInfoObject infoObject = (IInfoObject) users.get(0);
				IUser user = (IUser) infoObject;
				IUserAliases userAliases = user.getAliases();
				Iterator it = userAliases.iterator();
				
				while (it.hasNext()) 
				{
					IUserAlias userAlias = (IUserAlias) it.next();
					if (userAlias.isDisabled()) 
					{
						userAlias.setDisabled(false);
						unlock = true;
					}
				}
			} 
			else 
			{
				unlock = false;
			}
			
			iStore.commit(users);
			
		}
		catch (SDKException sdke) 
		{
			logger.error("SDK Exception recieved", sdke.getMessage());
			unlock=false;
		} 
		catch (Exception e) 
		{
			logger.error("Unlock user Exception recieved", e.getMessage());
			unlock=false;
		}
		finally
		{
			if (boEnterpriseSession!=null)
			{
				boEnterpriseSession.logoff();
			}
		}
		
		return unlock;
	}
}
