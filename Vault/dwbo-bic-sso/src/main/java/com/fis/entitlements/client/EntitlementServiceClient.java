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

package com.fis.entitlements.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;

import com.fis.bic.sso.utility.BicLogger;
import com.fis.entitlements.client.ETRetrieveUserApplCatAccessMtvnSvcRes.Svc;
import com.fis.entitlements.client.ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData;
import com.fis.entitlements.client.RetrieveUserApplCatAccessResponse.Org.PrcsGrp;

public class EntitlementServiceClient {

	private BicLogger logger = BicLogger.getLogger(this.getClass());
	private static final int MAX_RETRIES = 3;
	private static final int TIMEOUT = 5000; // milliseconds
	private static final int READ_TIMEOUT = 10000;
	
    private final MtvnETRetrieveUserApplCatAccessWSV1Interface port;

    public EntitlementServiceClient(String endpointUrl) {
    	// Load WSDL from classpath
    	URL wsdlUrl = EntitlementServiceClient.class.getClassLoader().getResource("wsdl/ETRetrieveUserApplCatAccessWSV1_SOAP12.wsdl");

    	// QName must match the WSDL's targetNamespace and service name
		QName serviceName = new QName("mtvnETRetrieveUserApplCatAccessWSV1", "mtvnETRetrieveUserApplCatAccessWSV1");

    	// Create the service
    	MtvnETRetrieveUserApplCatAccessWSV1 service = new MtvnETRetrieveUserApplCatAccessWSV1(wsdlUrl, serviceName);

    	// Get the port
    	this.port = service.getMtvnETRetrieveUserApplCatAccessWSV1Port();
      
        Map<String, Object> context = ((BindingProvider) port).getRequestContext();
      	
        // Set the end point dynamically
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);       
		// Set connection timeout (in milliseconds)
		context.put("com.sun.xml.internal.ws.connect.timeout", TIMEOUT);	
		// Set read timeout (in milliseconds)
		context.put("com.sun.xml.internal.ws.request.timeout", READ_TIMEOUT);

		//Set explicit logging
		Binding binding = ((BindingProvider) port).getBinding();
		List<Handler> handlerChain = new ArrayList<>();
		handlerChain.add(new SOAPLoggingHandler());
		binding.setHandlerChain(handlerChain);
    }

    public ETRetrieveUserApplCatAccessMtvnSvcRes retrieveUserAccess(ETRetrieveUserApplCatAccessMtvnSvcReq request) 
    {
		int attempt = 0;
		while (attempt < MAX_RETRIES) {
			try {
				ETRetrieveUserApplCatAccessMtvnSvcRes response = port.etRetrieveUserApplCatAccess(request);
				return response;
				
			} catch (WebServiceException e) {
				attempt++;
				logger.warn("Attempt " + attempt + " failed: " + e.getMessage(), e);
				
				if (attempt >= MAX_RETRIES) {
					logger.fatal("Max retries reached. Failing the request.");
					throw e;
				}
				try {
					TimeUnit.SECONDS.sleep(2); // Wait before retrying
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Thread interrupted during retry wait", ie);
				}
			}	
		}
		
		throw new RuntimeException("Failed to call the service after " + MAX_RETRIES + " attempts");
	}

    
    public Boolean UserHasApplId(String userId, String authKey, String bankNumber, String applId, String uuid)
    {
    	ObjectFactory factory = new ObjectFactory();
    	
    	ETRetrieveUserApplCatAccessMtvnSvcReq request = factory.createETRetrieveUserApplCatAccessMtvnSvcReq();
   	    	
    	request.msgUUID = uuid;
    	request.mtvnSvcVer = "String";
    	
    	request.prcsParms = factory.createETRetrieveUserApplCatAccessMtvnSvcReqPrcsParms();
    	request.prcsParms.srcID = "BIC";
    	
    	ETRetrieveUserApplCatAccessMtvnSvcReq.Svc serviceData = factory.createETRetrieveUserApplCatAccessMtvnSvcReqSvc();
    	serviceData.svcParms = new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms();
    	serviceData.svcParms.applID = "ET";
    	serviceData.svcParms.svcID = "ENTL_SVC14";
    	serviceData.svcParms.svcVer = "1.0";
    	serviceData.svcParms.svcNme = "RetrieveUserApplCatAccess";
    	serviceData.svcParms.rqstUUID =	uuid;
    	serviceData.svcParms.routingID = bankNumber;
    	
    	serviceData.security = factory.createETRetrieveUserApplCatAccessMtvnSvcReqSvcSecurity();
    	serviceData.security.basicAuth = factory.createETRetrieveUserApplCatAccessMtvnSvcReqSvcSecurityBasicAuth();
    	serviceData.security.basicAuth.usrID = userId;
    	serviceData.security.basicAuth.pwd = authKey;
    	
    	serviceData.msgData = factory.createETRetrieveUserApplCatAccessMtvnSvcReqSvcMsgData();
    	serviceData.msgData.retrieveUserApplCatAccessRequest = factory.createRetrieveUserApplCatAccessRequestType();
    	serviceData.msgData.retrieveUserApplCatAccessRequest.prcsGrpId = bankNumber;
    	
    	request.svc = new ArrayList<ETRetrieveUserApplCatAccessMtvnSvcReq.Svc>();
    	request.svc.add(serviceData);
    	
    	ETRetrieveUserApplCatAccessMtvnSvcRes response = retrieveUserAccess(request);
    			
    	// check for response errors
    	if (Integer.parseInt(response.errCde) != 0 || response.svc.stream().anyMatch(s -> Integer.parseInt(s.errCde) != 0))
    	{
    		logger.fatal("There was a problem completing request to entitlement service.");
    		return false;
    	}
    	
    	// if no errors found, traverse the response object and find the applId in the response
    	return response.getSvc().stream()
    		    .map(Svc::getMsgData)
    		    .filter(Objects::nonNull)
    		    .map(MsgData::getRetrieveUserApplCatAccessResponse)
    		    .filter(Objects::nonNull)
    		    .flatMap(resp -> resp.getOrg().stream())
    		    .flatMap(org -> org.getPrcsGrp().stream())
    		    .filter(pg -> pg.prcsGrpId.equalsIgnoreCase(bankNumber)) // filter on specific bank number
    		    .map(PrcsGrp::getEntlList)
    		    .filter(Objects::nonNull)
    		    .flatMap(entlList -> entlList.getEntlAppl().stream())
    		    .anyMatch(entlAppl -> applId.equals(entlAppl.getApplId()));
	}
}