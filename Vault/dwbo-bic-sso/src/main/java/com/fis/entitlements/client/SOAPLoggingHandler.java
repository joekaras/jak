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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.fis.bic.sso.utility.BicLogger;

/***
 * SOAP handler that logs request and response from soap interaction.
 */
public class SOAPLoggingHandler implements SOAPHandler<SOAPMessageContext> {
    
    private final BicLogger logger = BicLogger.getLogger(this.getClass());
    
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        logger.info(outbound ? "Outbound SOAP message(Sending request):" : "Inbound SOAP message(Received response):");
        
        // Log the SOAP envelope
        try {
            ByteArrayOutputStream soapXmlStream = new ByteArrayOutputStream();
            context.getMessage().writeTo(soapXmlStream);
            String soapXml = new String(soapXmlStream.toByteArray(), StandardCharsets.UTF_8);
            
            // sanitize pwd field so that it doesn't get written to log files.
            String sanitizedSoapXml = soapXml.replaceAll("(?i)(<Pwd>)(.*?)(</Pwd>)", "$1****$3");
            logger.info(sanitizedSoapXml);
        } catch (Exception e) {
            logger.error("Error logging SOAP message: ", e);
        }
        
     // Log HTTP headers
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
        if (headers != null) {
            StringBuilder headerLog = new StringBuilder("HTTP Headers:\n");
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                headerLog.append(entry.getKey())
                         .append(": ")
                         .append(String.join(", ", entry.getValue()))
                         .append("\n");
            }
            logger.info(headerLog.toString());
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        logger.error("SOAP Fault:");
        try {
            ByteArrayOutputStream soapXmlStream = new ByteArrayOutputStream();
            context.getMessage().writeTo(soapXmlStream);
            String soapXml = new String(soapXmlStream.toByteArray(), StandardCharsets.UTF_8);
            
            logger.error(soapXml);
        } catch (Exception e) {
            logger.error("Error logging SOAP fault: ", e);
        }
        
        return true;
    }

    @Override
    public void close(MessageContext context) {
        // No resources to close
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}