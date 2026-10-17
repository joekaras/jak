<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="com.fis.bic.sso.helper.SlapiTokenHandler"%> 
<%@ page import="com.fis.bic.sso.helper.SingleSignOnEvents"%>
<%@ page import="com.fis.bic.sso.helper.SingleSignOnConfiguration"%>
<%@ page import="com.fis.slapi.login.Credentials"%>
<%@ page import="com.fis.bic.sso.helper.LoginHelper"%>
<%@ page import="com.fis.bic.sso.utility.BicLogger"%>
<%@ page import="com.fis.bic.sso.utility.BicssoConstants"%>
<%@page import="com.fis.bic.sso.utility.LoginRequest"%>
<%@page import="com.fis.bic.sso.utility.ILogin"%>
<%@page import="java.net.URL"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.owasp.encoder.Encode"%>
<%@page import="com.fis.entitlements.client.EntitlementServiceClient" %>

<%
	BicLogger logger = BicLogger.getLogger(this.getClass());

	SingleSignOnConfiguration configuration = SingleSignOnConfiguration.getSingletonObject();
	if(configuration.getSignOnMode().equals(BicssoConstants.CHANNEL_SSO_MODE)){
		logger.error("Single Sign on mode set as  " + BicssoConstants.CHANNEL_SSO_MODE+" You are not authorized to launch the application ");
		return;
	}
	SlapiTokenHandler slapihelper = SlapiTokenHandler.getSingletonObject();
	SingleSignOnEvents signOnEvnt = SingleSignOnEvents.getSingletonObject();
	ServletContext context= getServletContext();
	LoginHelper login = null;
	LoginRequest loginResult = null;
	String action = null;
	String authToken = null;
	String keyToken = null;
	String channel = null;
	String redirectURL = null;
	String userid = null;
	String logonToken = null;
	String ssoFailed = "fatalErrorPage.jsp";
	//CMC variables
	String address="";
	String userPassword="";
	String cmsName="";
	
	authToken = request.getParameter("authtkn");
	keyToken =  request.getParameter("keytkn");
	channel =  request.getParameter("channel");
	

	logger.debug("Received SLAPI tokens - channel=." + channel);
	
	if (authToken == null || authToken.trim().length() == 0) {
		logger.error("SSOTKN", "Slapi AuthToken value is missing"); 
		response.sendRedirect(ssoFailed);
		return;		
	}
	if (keyToken == null || keyToken.trim().length() == 0) {
		logger.error("SSOTKN", "Slapi KeyToken value is missing");
		response.sendRedirect(ssoFailed);
		return;		
	}
	if (channel == null || channel.trim().length() == 0) {
		logger.error("SSOTKN", "Slapi channel name is missing");
		response.sendRedirect(ssoFailed);
		return;		
	}

	Credentials authCredentials;
	
	// Extract token from hidden sso request
	try 
	{
		Credentials cr = new Credentials();
		cr.addCredential(Credentials.KEY_KEYTOKEN, keyToken);
		cr.addCredential(Credentials.KEY_AUTHTOKEN, authToken);
		cr.addCredential(Credentials.CHANNELNAME,channel);
		cr.addCredential(Credentials.KEY_CRYPTOGRAPHYHASHALGORITHM, slapihelper.getCryptographyHashAlgorithm(channel));
		cr.addCredential(Credentials.CRYPTOSTRATEGY,slapihelper.getCryptoStrategy(channel));
		
		String slapiXmlToken = cr.getFederatedSecurityToken();
		logger.debug("Extracted SSOTKN", slapiXmlToken);
		
		authCredentials = slapihelper.getDecryptedCredentials(slapiXmlToken, channel);
		userid = authCredentials.getCredential(Credentials.KEY_USERID);
		logger.debug("Extracted userid", userid);
	} 
	catch (Exception decrypt) 
	{
		logger.error("SSOTKN -Failed to Decrypt token",decrypt.getMessage());
		response.sendRedirect(ssoFailed);
		return;	
	}

	if(userid == null) 
	{
		logger.error("Single Sign On failed: there is no user name provided.");
		response.sendRedirect(ssoFailed);
		return;	
	}
	
	try {
	
		request.setAttribute("userName", userid);
		request.setAttribute("channelName", channel);
		
		if (channel.equals(BicssoConstants.LAUNCHPADSSO)
					|| channel.equals(BicssoConstants.CUSTOMERMINERSSO) ) {
			
			//validate Entitlment ApplId
			String entitlementEndpoint = configuration.getEntitlementUrl();
			EntitlementServiceClient client = new EntitlementServiceClient(entitlementEndpoint);
			
			String transUuid = (String)request.getAttribute("UUID");
			if (transUuid == null) {
				transUuid = java.util.UUID.randomUUID().toString();
			}
			
			java.util.List<String> banksExemptFromEntitlementCheck = configuration.getEntitlementBankExceptions();
			String userBank = userid.substring(1,4);
			
			String applId = channel.equals(BicssoConstants.LAUNCHPADSSO) ? configuration.getBusinessObjectsApplId() : configuration.getCustomerMinerApplId();
			
			// If the user's bank is not on the exception list and the user does not have the proper entitlement role, access is denied.
			if (banksExemptFromEntitlementCheck.stream().noneMatch(bank -> bank.equalsIgnoreCase(userBank)) && !client.UserHasApplId(userid, 
					authCredentials.getCredential(Credentials.KEY_CREDENTIAL),
					userBank,
					applId, 
					transUuid))
			{
				throw new Exception("User does not have proper entitlments ApplId assigned to access application.");
			}
			
			login = new LoginHelper();
			signOnEvnt.setLogin(login);
			loginResult = login.TokenLoginBO4(userid);
			if ( loginResult == null )
			{
				throw new Exception("Login Exception");
			}
			logger.info("Launchpad SSO Single Sign On successful for user:", userid);

			redirectURL =Encode.forHtml(configuration.getBic4ServerName() + configuration.getBic4LaunchPadPath());
			logger.info("Redirecting to Launchpad url: ", redirectURL);
			
			logonToken = loginResult.LoginToken;

			URL check = new URL(redirectURL);
			logger.debug("Redirect to: " + redirectURL);
		} else if ( channel.equals(BicssoConstants.MAC4SSO) ) {
			// Mythos remediation: Legacy Vulnerability ID a6640101-62aa-432c-b283-f7c87c04cc43; SNOW AVIT32772774
			//validate Entitlment ApplId
			String entitlementEndpoint = configuration.getEntitlementUrl();
			EntitlementServiceClient client = new EntitlementServiceClient(entitlementEndpoint);

			String transUuid = (String)request.getAttribute("UUID");
			if (transUuid == null) {
				transUuid = java.util.UUID.randomUUID().toString();
			}

			java.util.List<String> banksExemptFromEntitlementCheck = configuration.getEntitlementBankExceptions();
			String userBank = userid.substring(1,4);

			String applId = configuration.getMac4ApplId();
			if (applId == null || applId.trim().length() == 0) {
				throw new Exception("No entitlement ApplId configured for channel " + channel + ". Access denied.");
			}

			// If the user's bank is not on the exception list and the user does not have the proper entitlement role, access is denied.
			if (banksExemptFromEntitlementCheck.stream().noneMatch(bank -> bank.equalsIgnoreCase(userBank)) && !client.UserHasApplId(userid,
					authCredentials.getCredential(Credentials.KEY_CREDENTIAL),
					userBank,
					applId,
					transUuid))
			{
				throw new Exception("User does not have proper entitlments ApplId assigned to access application.");
			}

			login = new LoginHelper();
			signOnEvnt.setLogin(login);
			loginResult = login.TokenLoginBO4(userid);
			if ( loginResult == null )
			{
				throw new Exception("Login Exception");
			}
			logger.info("Launchpad SSO Single Sign On successful for user:", userid);
			redirectURL =Encode.forHtml(configuration.getBic4ServerName() + configuration.getMacsso4LaunchPadPath());
			logonToken = loginResult.LoginToken;

			URL check = new URL(redirectURL);
			logger.debug("Redirect to: " + redirectURL);
		} else {
			throw new Exception("Unknown Channel name :" + channel);
		}						

	} catch (Exception launchExp) {
		logger.error("SSOTKN -Single Sign On failed", launchExp);	
		response.sendRedirect(ssoFailed);
		return;		
	}
	
	logger.info("BO 4.x application ", channel);

%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<title>SSO Redirect</title>
<script type="text/javascript">function submitForm() { document.getElementsByTagName('form')[0].submit(); } window.onload = submitForm;</script>
<meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self'; style-src 'self'; frame-ancestors 'none'; object-src 'none';">
</head>
<body>
	<div align='center'
		style='font-family: Arial, Helvetica, sans-serif; font-size: 130%; margin-top: 75px;'>
		<p>Retrieving Information...</p>
	</div>
<form method="POST" action=<%=redirectURL%>>
    <input type="hidden" name="ivsLogonToken" value=<%=logonToken%> />
</form>
</body>
</html>
