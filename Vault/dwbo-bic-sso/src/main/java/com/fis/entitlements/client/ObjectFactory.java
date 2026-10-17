/*
===================================================================
Copyright Notice!
This document is protected under the trade secret and copyright
laws as the property of Fidelity National Information Services, Inc.
Copying, reproduction or distribution should be limited and only to
employees with a "need to know" to do their job.
Any disclosure of this document to third parties
is strictly prohibited.
copyright 2025 Fidelity National Information Services
All rights reserved worldwide.
===================================================================
*/

package com.fis.entitlements.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.fis.entitlements.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _UsrId_QNAME = new QName("mtvnETRetrieveUserApplCatAccessReqData", "UsrId");
    private final static QName _RetrieveUserApplCatAccessRequest_QNAME = new QName("mtvnETRetrieveUserApplCatAccessReqData", "RetrieveUserApplCatAccessRequest");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.fis.entitlements.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq createETRetrieveUserApplCatAccessMtvnSvcReq() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcRes }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcRes createETRetrieveUserApplCatAccessMtvnSvcRes() {
        return new ETRetrieveUserApplCatAccessMtvnSvcRes();
    }

    /**
     * Create an instance of {@link RetrieveUserApplCatAccessResponse }
     * 
     */
    public RetrieveUserApplCatAccessResponse createRetrieveUserApplCatAccessResponse() {
        return new RetrieveUserApplCatAccessResponse();
    }

    /**
     * Create an instance of {@link RetrieveUserApplCatAccessResponse.Org }
     * 
     */
    public RetrieveUserApplCatAccessResponse.Org createRetrieveUserApplCatAccessResponseOrg() {
        return new RetrieveUserApplCatAccessResponse.Org();
    }

    /**
     * Create an instance of {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp }
     * 
     */
    public RetrieveUserApplCatAccessResponse.Org.PrcsGrp createRetrieveUserApplCatAccessResponseOrgPrcsGrp() {
        return new RetrieveUserApplCatAccessResponse.Org.PrcsGrp();
    }

    /**
     * Create an instance of {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList }
     * 
     */
    public RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList createRetrieveUserApplCatAccessResponseOrgPrcsGrpEntlList() {
        return new RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList();
    }

    /**
     * Create an instance of {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl }
     * 
     */
    public RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl createRetrieveUserApplCatAccessResponseOrgPrcsGrpEntlListEntlAppl() {
        return new RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcRes.Svc createETRetrieveUserApplCatAccessMtvnSvcResSvc() {
        return new ETRetrieveUserApplCatAccessMtvnSvcRes.Svc();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc createETRetrieveUserApplCatAccessMtvnSvcReqSvc() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security createETRetrieveUserApplCatAccessMtvnSvcReqSvcSecurity() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms createETRetrieveUserApplCatAccessMtvnSvcReqSvcSvcParms() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src createETRetrieveUserApplCatAccessMtvnSvcReqSvcSvcParmsSrc() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src.FieldList }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src.FieldList createETRetrieveUserApplCatAccessMtvnSvcReqSvcSvcParmsSrcFieldList() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src.FieldList();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.PrcsParms }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.PrcsParms createETRetrieveUserApplCatAccessMtvnSvcReqPrcsParms() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.PrcsParms();
    }

    /**
     * Create an instance of {@link RetrieveUserApplCatAccessRequestType }
     * 
     */
    public RetrieveUserApplCatAccessRequestType createRetrieveUserApplCatAccessRequestType() {
        return new RetrieveUserApplCatAccessRequestType();
    }

    /**
     * Create an instance of {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl.EntlCat }
     * 
     */
    public RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl.EntlCat createRetrieveUserApplCatAccessResponseOrgPrcsGrpEntlListEntlApplEntlCat() {
        return new RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl.EntlCat();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms createETRetrieveUserApplCatAccessMtvnSvcResSvcSvcParms() {
        return new ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData createETRetrieveUserApplCatAccessMtvnSvcResSvcMsgData() {
        return new ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.MsgData }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.MsgData createETRetrieveUserApplCatAccessMtvnSvcReqSvcMsgData() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.MsgData();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.BasicAuth }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.BasicAuth createETRetrieveUserApplCatAccessMtvnSvcReqSvcSecurityBasicAuth() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.BasicAuth();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.SessionAuth }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.SessionAuth createETRetrieveUserApplCatAccessMtvnSvcReqSvcSecuritySessionAuth() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.SessionAuth();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.SAMLAuth }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.SAMLAuth createETRetrieveUserApplCatAccessMtvnSvcReqSvcSecuritySAMLAuth() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.Security.SAMLAuth();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src.FieldList.Field }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src.FieldList.Field createETRetrieveUserApplCatAccessMtvnSvcReqSvcSvcParmsSrcFieldListField() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.Svc.SvcParms.Src.FieldList.Field();
    }

    /**
     * Create an instance of {@link ETRetrieveUserApplCatAccessMtvnSvcReq.PrcsParms.PrcsRule }
     * 
     */
    public ETRetrieveUserApplCatAccessMtvnSvcReq.PrcsParms.PrcsRule createETRetrieveUserApplCatAccessMtvnSvcReqPrcsParmsPrcsRule() {
        return new ETRetrieveUserApplCatAccessMtvnSvcReq.PrcsParms.PrcsRule();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "mtvnETRetrieveUserApplCatAccessReqData", name = "UsrId")
    public JAXBElement<String> createUsrId(String value) {
        return new JAXBElement<String>(_UsrId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RetrieveUserApplCatAccessRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "mtvnETRetrieveUserApplCatAccessReqData", name = "RetrieveUserApplCatAccessRequest")
    public JAXBElement<RetrieveUserApplCatAccessRequestType> createRetrieveUserApplCatAccessRequest(RetrieveUserApplCatAccessRequestType value) {
        return new JAXBElement<RetrieveUserApplCatAccessRequestType>(_RetrieveUserApplCatAccessRequest_QNAME, RetrieveUserApplCatAccessRequestType.class, null, value);
    }

}
