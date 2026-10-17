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
 * copyright 2025 Fidelity National Information Services
 * All rights reserved worldwide.
 * ===================================================================
 */

package com.fis.bic.sso.helper;

import java.util.Arrays;
import java.util.List;
import com.fis.bic.sso.utility.BicLogger;
import com.fis.bic.sso.utility.BicssoConstants;
import com.fis.bic.sso.utility.EncryptedProperties;
import com.fis.bic.sso.utility.ISingleSignOnConfiguration;

public class SingleSignOnConfiguration implements ISingleSignOnConfiguration,
		BicssoConstants {

	private static SingleSignOnConfiguration ssoconfigref;
	private static BicLogger logger = BicLogger.getLogger(SingleSignOnConfiguration.class);
	public EncryptedProperties encprop;
	private String administratorBO4UserName = null;
	private String signOnMode = null;
	private String cms4Name = null;
	private String bic4ServerName = null;
	private String bic4LaunchPadPath = null;
	private String macsso4LaunchPadPath=null;
	private String entitlementUrl=null;
	private String boApplId=null; //Business Objects Application Id
	private String cmApplId=null; //Marketing Suite Customer Miner Application Id
	private String mac4ApplId=null; //MAC 4 Application Id

	public SingleSignOnConfiguration() {
		encprop = EncryptedProperties
				.getSingletonObject(PROPERTY_LOOKUP_FILENAME);
	}

	public static SingleSignOnConfiguration getSingletonObject() {
		if (ssoconfigref == null) {
			logger.warn(
					"Created new instatnce of the SingleSignOnConfiguration singleton instance. Should only happen at servlet startup.",
					SingleSignOnConfiguration.class);
			ssoconfigref = new SingleSignOnConfiguration();
		}
		return ssoconfigref;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	/***
	 * Gets the BusinessObject Entitlement ApplId 
	 */
	public String getBusinessObjectsApplId() {
		boApplId = encprop.getProperty("BoApplId");
		logger.info("Returning configured BusinessObjects ApplId from storage", boApplId);
		return boApplId;
	}
	
	/***
	 * Gets the Marketing Suite Customer Miner Entitlement ApplId 
	 */
	public String getCustomerMinerApplId() {
		cmApplId = encprop.getProperty("CustomerMinerApplId");
		logger.info("Returning configured CustomerMiner ApplId from storage", cmApplId);
		return cmApplId;
	}

	// Mythos remediation: Legacy Vulnerability ID a6640101-62aa-432c-b283-f7c87c04cc43; SNOW AVIT32772774
	/***
	 * Gets the MAC 4 Entitlement ApplId
	 */
	public String getMac4ApplId() {
		mac4ApplId = encprop.getProperty("Mac4ApplId");
		logger.info("Returning configured Mac4 ApplId from storage", mac4ApplId);
		return mac4ApplId;
	}

	/***
	 * Get the Entitlement connectware service URI
	 */
	public String getEntitlementUrl() {
		entitlementUrl = encprop.getProperty("entitltment.endpoint.url");
		logger.info("Returning configured Entitlement Service Url from storage", entitlementUrl);
		return entitlementUrl;
	}
	
	/***
	 * Gets a list of banks that exempt users from those banks from needing the proper entitlement role check.
	 * @return list of banks used to bypass entitlement check for its users.
	 */
	public List<String> getEntitlementBankExceptions(){
		String bankList = encprop.getProperty("entitlement.exemption.list");
		logger.info("Returning configured Entitlement bank exeption list from storage", bankList);
		return Arrays.asList(bankList.split(","));
	}
	
	/*
	 * Reads value of CMS Name from application configuration file or returns
	 * previously read and cached value.
	 * 
	 * @see
	 * com.fis.bic.sso.utility.ISingleSignOnConfiguration#CMSName(java.lang
	 * .String)
	 * 
	 * @return Configured CMS Name or null if not configured.
	 */
	public String getCMS4Name() {
		cms4Name = encprop.getProperty("CMS4Name");
		logger.info("Returning configured CMS4 Name from storage", cms4Name);
		return cms4Name;
	}

	/*
	 * Reads value of SSO mode from application configuration file or returns
	 * previously read and cached value.
	 * 
	 * @see
	 * com.fis.bic.sso.utility.ISingleSignOnConfiguration#SignOnMode(java.lang
	 * .String)
	 * 
	 * @return Configured SSO mode or null if not configured.
	 */
	public String getSignOnMode() {
		signOnMode = encprop.getProperty("SSOMode");
		logger.info("Returning configured SSO mode from storage", signOnMode);
		return signOnMode;
	}

	/*
	 * Stores new value for SSO mode in application configuration file.
	 * 
	 * @see
	 * com.fis.bic.sso.utility.ISingleSignOnConfiguration#SetSignOnMode(java
	 * .lang.String)
	 * 
	 * @return Void
	 */
	public void setSignOnMode(final String newssoMode) {
		EncryptedProperties.setProperty("SSOMode",newssoMode);
		logger.info("Returning configured SSO mode from storage", signOnMode);		
		signOnMode = newssoMode;
	}

	/*
	 * Forces to re-read data from configuration file and to update cached
	 * value.
	 * 
	 * @see
	 * com.fis.bic.sso.utility.ISingleSignOnConfiguration#UpdateCachedMode()
	 * 
	 * @return void
	 */
	public void updateCachedMode() {
		// TODO Auto-generated method stub

	}

	/*
	 * Reads/writes value of BO Admin login name from
	 * 
	 * @see
	 * com.fis.bic.sso.utility.ISingleSignOnConfiguration#SignOnMode(java.lang
	 * .String)
	 * 
	 * @return Configured Admin UserName or null if not configured.
	 */
	public String getAdministratorBO4UserName() {
		administratorBO4UserName = encprop.getProperty("AdminBO4User");
		return administratorBO4UserName;
	}
	
	/*
	 * Reads the value of the BIC 4.X server name from
	 * 
	 * @see
	 * com.fis.bic.sso.utility.ISingleSignOnConfiguration#SignOnMode(java.lang
	 * .String)
	 * 
	 * @return Configured BIC4 server name or null if not configured.
	 */
	
	public String getBic4ServerName() {
		bic4ServerName = encprop.getProperty("BIC4ServerName");
		return bic4ServerName;
	}
	
	public String getBic4LaunchPadPath() {
		bic4LaunchPadPath = encprop.getProperty("BIC4LaunchPadPath");
		return bic4LaunchPadPath;
	}

	public String getMacsso4LaunchPadPath() {
		macsso4LaunchPadPath = encprop.getProperty("MACSSO4LaunchPadPath");
		return macsso4LaunchPadPath;
	}
	
}
