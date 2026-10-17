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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="LstNme" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="FrstNme" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="EmpId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="PwdManaged" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="SecurityUser" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="Org" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="OrgNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="PrcsGrp" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="PrcsGrpId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="EntrprseOrgNme" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                             &lt;element name="EntlList" minOccurs="0">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;sequence>
 *                                       &lt;element name="EntlAppl" maxOccurs="unbounded" minOccurs="0">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="ApplId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                                 &lt;element name="ApplNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                                 &lt;element name="EntlCat" maxOccurs="unbounded">
 *                                                   &lt;complexType>
 *                                                     &lt;complexContent>
 *                                                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                         &lt;sequence>
 *                                                           &lt;element name="CatId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                                           &lt;element name="CatNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                                         &lt;/sequence>
 *                                                       &lt;/restriction>
 *                                                     &lt;/complexContent>
 *                                                   &lt;/complexType>
 *                                                 &lt;/element>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                       &lt;element name="ErrCde" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                       &lt;element name="ErrMsg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                                     &lt;/sequence>
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                             &lt;element name="ErrCde" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="ErrMsg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "lstNme",
    "frstNme",
    "empId",
    "pwdManaged",
    "securityUser",
    "org"
})
@XmlRootElement(name = "RetrieveUserApplCatAccessResponse", namespace = "mtvnETRetrieveUserApplCatAccessResData")
public class RetrieveUserApplCatAccessResponse {

    @XmlElement(name = "LstNme", namespace = "mtvnETRetrieveUserApplCatAccessResData")
    protected String lstNme;
    @XmlElement(name = "FrstNme", namespace = "mtvnETRetrieveUserApplCatAccessResData")
    protected String frstNme;
    @XmlElement(name = "EmpId", namespace = "mtvnETRetrieveUserApplCatAccessResData")
    protected String empId;
    @XmlElement(name = "PwdManaged", namespace = "mtvnETRetrieveUserApplCatAccessResData")
    protected Boolean pwdManaged;
    @XmlElement(name = "SecurityUser", namespace = "mtvnETRetrieveUserApplCatAccessResData")
    protected Boolean securityUser;
    @XmlElement(name = "Org", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
    protected List<RetrieveUserApplCatAccessResponse.Org> org;

    /**
     * Gets the value of the lstNme property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLstNme() {
        return lstNme;
    }

    /**
     * Sets the value of the lstNme property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLstNme(String value) {
        this.lstNme = value;
    }

    /**
     * Gets the value of the frstNme property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFrstNme() {
        return frstNme;
    }

    /**
     * Sets the value of the frstNme property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFrstNme(String value) {
        this.frstNme = value;
    }

    /**
     * Gets the value of the empId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmpId() {
        return empId;
    }

    /**
     * Sets the value of the empId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmpId(String value) {
        this.empId = value;
    }

    /**
     * Gets the value of the pwdManaged property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPwdManaged() {
        return pwdManaged;
    }

    /**
     * Sets the value of the pwdManaged property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPwdManaged(Boolean value) {
        this.pwdManaged = value;
    }

    /**
     * Gets the value of the securityUser property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSecurityUser() {
        return securityUser;
    }

    /**
     * Sets the value of the securityUser property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSecurityUser(Boolean value) {
        this.securityUser = value;
    }

    /**
     * Gets the value of the org property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the org property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrg().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RetrieveUserApplCatAccessResponse.Org }
     * 
     * 
     */
    public List<RetrieveUserApplCatAccessResponse.Org> getOrg() {
        if (org == null) {
            org = new ArrayList<RetrieveUserApplCatAccessResponse.Org>();
        }
        return this.org;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="OrgNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="PrcsGrp" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="PrcsGrpId" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                   &lt;element name="EntrprseOrgNme" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *                   &lt;element name="EntlList" minOccurs="0">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;sequence>
     *                             &lt;element name="EntlAppl" maxOccurs="unbounded" minOccurs="0">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="ApplId" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                                       &lt;element name="ApplNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                                       &lt;element name="EntlCat" maxOccurs="unbounded">
     *                                         &lt;complexType>
     *                                           &lt;complexContent>
     *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                               &lt;sequence>
     *                                                 &lt;element name="CatId" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                                                 &lt;element name="CatNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                                               &lt;/sequence>
     *                                             &lt;/restriction>
     *                                           &lt;/complexContent>
     *                                         &lt;/complexType>
     *                                       &lt;/element>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                             &lt;element name="ErrCde" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                             &lt;element name="ErrMsg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *                           &lt;/sequence>
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                   &lt;element name="ErrCde" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                   &lt;element name="ErrMsg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *                 &lt;/sequence>
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "orgNme",
        "prcsGrp"
    })
    public static class Org {

        @XmlElement(name = "OrgNme", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
        protected String orgNme;
        @XmlElement(name = "PrcsGrp", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
        protected List<RetrieveUserApplCatAccessResponse.Org.PrcsGrp> prcsGrp;

        /**
         * Gets the value of the orgNme property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOrgNme() {
            return orgNme;
        }

        /**
         * Sets the value of the orgNme property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOrgNme(String value) {
            this.orgNme = value;
        }

        /**
         * Gets the value of the prcsGrp property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the prcsGrp property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getPrcsGrp().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp }
         * 
         * 
         */
        public List<RetrieveUserApplCatAccessResponse.Org.PrcsGrp> getPrcsGrp() {
            if (prcsGrp == null) {
                prcsGrp = new ArrayList<RetrieveUserApplCatAccessResponse.Org.PrcsGrp>();
            }
            return this.prcsGrp;
        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType>
         *   &lt;complexContent>
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *       &lt;sequence>
         *         &lt;element name="PrcsGrpId" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *         &lt;element name="EntrprseOrgNme" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
         *         &lt;element name="EntlList" minOccurs="0">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;sequence>
         *                   &lt;element name="EntlAppl" maxOccurs="unbounded" minOccurs="0">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="ApplId" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                             &lt;element name="ApplNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                             &lt;element name="EntlCat" maxOccurs="unbounded">
         *                               &lt;complexType>
         *                                 &lt;complexContent>
         *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                                     &lt;sequence>
         *                                       &lt;element name="CatId" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                                       &lt;element name="CatNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                                     &lt;/sequence>
         *                                   &lt;/restriction>
         *                                 &lt;/complexContent>
         *                               &lt;/complexType>
         *                             &lt;/element>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                   &lt;element name="ErrCde" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                   &lt;element name="ErrMsg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
         *                 &lt;/sequence>
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *         &lt;element name="ErrCde" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *         &lt;element name="ErrMsg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
         *       &lt;/sequence>
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "prcsGrpId",
            "entrprseOrgNme",
            "entlList",
            "errCde",
            "errMsg"
        })
        public static class PrcsGrp {

            @XmlElement(name = "PrcsGrpId", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
            protected String prcsGrpId;
            @XmlElement(name = "EntrprseOrgNme", namespace = "mtvnETRetrieveUserApplCatAccessResData")
            protected String entrprseOrgNme;
            @XmlElement(name = "EntlList", namespace = "mtvnETRetrieveUserApplCatAccessResData")
            protected RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList entlList;
            @XmlElement(name = "ErrCde", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
            protected String errCde;
            @XmlElement(name = "ErrMsg", namespace = "mtvnETRetrieveUserApplCatAccessResData")
            protected String errMsg;

            /**
             * Gets the value of the prcsGrpId property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getPrcsGrpId() {
                return prcsGrpId;
            }

            /**
             * Sets the value of the prcsGrpId property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setPrcsGrpId(String value) {
                this.prcsGrpId = value;
            }

            /**
             * Gets the value of the entrprseOrgNme property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEntrprseOrgNme() {
                return entrprseOrgNme;
            }

            /**
             * Sets the value of the entrprseOrgNme property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEntrprseOrgNme(String value) {
                this.entrprseOrgNme = value;
            }

            /**
             * Gets the value of the entlList property.
             * 
             * @return
             *     possible object is
             *     {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList }
             *     
             */
            public RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList getEntlList() {
                return entlList;
            }

            /**
             * Sets the value of the entlList property.
             * 
             * @param value
             *     allowed object is
             *     {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList }
             *     
             */
            public void setEntlList(RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList value) {
                this.entlList = value;
            }

            /**
             * Gets the value of the errCde property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getErrCde() {
                return errCde;
            }

            /**
             * Sets the value of the errCde property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setErrCde(String value) {
                this.errCde = value;
            }

            /**
             * Gets the value of the errMsg property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getErrMsg() {
                return errMsg;
            }

            /**
             * Sets the value of the errMsg property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setErrMsg(String value) {
                this.errMsg = value;
            }


            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected content contained within this class.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;sequence>
             *         &lt;element name="EntlAppl" maxOccurs="unbounded" minOccurs="0">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="ApplId" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *                   &lt;element name="ApplNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *                   &lt;element name="EntlCat" maxOccurs="unbounded">
             *                     &lt;complexType>
             *                       &lt;complexContent>
             *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                           &lt;sequence>
             *                             &lt;element name="CatId" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *                             &lt;element name="CatNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *                           &lt;/sequence>
             *                         &lt;/restriction>
             *                       &lt;/complexContent>
             *                     &lt;/complexType>
             *                   &lt;/element>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *         &lt;element name="ErrCde" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *         &lt;element name="ErrMsg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
             *       &lt;/sequence>
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "entlAppl",
                "errCde",
                "errMsg"
            })
            public static class EntlList {

                @XmlElement(name = "EntlAppl", namespace = "mtvnETRetrieveUserApplCatAccessResData")
                protected List<RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl> entlAppl;
                @XmlElement(name = "ErrCde", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
                protected String errCde;
                @XmlElement(name = "ErrMsg", namespace = "mtvnETRetrieveUserApplCatAccessResData")
                protected String errMsg;

                /**
                 * Gets the value of the entlAppl property.
                 * 
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the JAXB object.
                 * This is why there is not a <CODE>set</CODE> method for the entlAppl property.
                 * 
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getEntlAppl().add(newItem);
                 * </pre>
                 * 
                 * 
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl }
                 * 
                 * 
                 */
                public List<RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl> getEntlAppl() {
                    if (entlAppl == null) {
                        entlAppl = new ArrayList<RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl>();
                    }
                    return this.entlAppl;
                }

                /**
                 * Gets the value of the errCde property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getErrCde() {
                    return errCde;
                }

                /**
                 * Sets the value of the errCde property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setErrCde(String value) {
                    this.errCde = value;
                }

                /**
                 * Gets the value of the errMsg property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getErrMsg() {
                    return errMsg;
                }

                /**
                 * Sets the value of the errMsg property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setErrMsg(String value) {
                    this.errMsg = value;
                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;sequence>
                 *         &lt;element name="ApplId" type="{http://www.w3.org/2001/XMLSchema}string"/>
                 *         &lt;element name="ApplNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
                 *         &lt;element name="EntlCat" maxOccurs="unbounded">
                 *           &lt;complexType>
                 *             &lt;complexContent>
                 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *                 &lt;sequence>
                 *                   &lt;element name="CatId" type="{http://www.w3.org/2001/XMLSchema}string"/>
                 *                   &lt;element name="CatNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
                 *                 &lt;/sequence>
                 *               &lt;/restriction>
                 *             &lt;/complexContent>
                 *           &lt;/complexType>
                 *         &lt;/element>
                 *       &lt;/sequence>
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "applId",
                    "applNme",
                    "entlCat"
                })
                public static class EntlAppl {

                    @XmlElement(name = "ApplId", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
                    protected String applId;
                    @XmlElement(name = "ApplNme", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
                    protected String applNme;
                    @XmlElement(name = "EntlCat", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
                    protected List<RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl.EntlCat> entlCat;

                    /**
                     * Gets the value of the applId property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    public String getApplId() {
                        return applId;
                    }

                    /**
                     * Sets the value of the applId property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    public void setApplId(String value) {
                        this.applId = value;
                    }

                    /**
                     * Gets the value of the applNme property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    public String getApplNme() {
                        return applNme;
                    }

                    /**
                     * Sets the value of the applNme property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    public void setApplNme(String value) {
                        this.applNme = value;
                    }

                    /**
                     * Gets the value of the entlCat property.
                     * 
                     * <p>
                     * This accessor method returns a reference to the live list,
                     * not a snapshot. Therefore any modification you make to the
                     * returned list will be present inside the JAXB object.
                     * This is why there is not a <CODE>set</CODE> method for the entlCat property.
                     * 
                     * <p>
                     * For example, to add a new item, do as follows:
                     * <pre>
                     *    getEntlCat().add(newItem);
                     * </pre>
                     * 
                     * 
                     * <p>
                     * Objects of the following type(s) are allowed in the list
                     * {@link RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl.EntlCat }
                     * 
                     * 
                     */
                    public List<RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl.EntlCat> getEntlCat() {
                        if (entlCat == null) {
                            entlCat = new ArrayList<RetrieveUserApplCatAccessResponse.Org.PrcsGrp.EntlList.EntlAppl.EntlCat>();
                        }
                        return this.entlCat;
                    }


                    /**
                     * <p>Java class for anonymous complex type.
                     * 
                     * <p>The following schema fragment specifies the expected content contained within this class.
                     * 
                     * <pre>
                     * &lt;complexType>
                     *   &lt;complexContent>
                     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                     *       &lt;sequence>
                     *         &lt;element name="CatId" type="{http://www.w3.org/2001/XMLSchema}string"/>
                     *         &lt;element name="CatNme" type="{http://www.w3.org/2001/XMLSchema}string"/>
                     *       &lt;/sequence>
                     *     &lt;/restriction>
                     *   &lt;/complexContent>
                     * &lt;/complexType>
                     * </pre>
                     * 
                     * 
                     */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                        "catId",
                        "catNme"
                    })
                    public static class EntlCat {

                        @XmlElement(name = "CatId", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
                        protected String catId;
                        @XmlElement(name = "CatNme", namespace = "mtvnETRetrieveUserApplCatAccessResData", required = true)
                        protected String catNme;

                        /**
                         * Gets the value of the catId property.
                         * 
                         * @return
                         *     possible object is
                         *     {@link String }
                         *     
                         */
                        public String getCatId() {
                            return catId;
                        }

                        /**
                         * Sets the value of the catId property.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link String }
                         *     
                         */
                        public void setCatId(String value) {
                            this.catId = value;
                        }

                        /**
                         * Gets the value of the catNme property.
                         * 
                         * @return
                         *     possible object is
                         *     {@link String }
                         *     
                         */
                        public String getCatNme() {
                            return catNme;
                        }

                        /**
                         * Sets the value of the catNme property.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link String }
                         *     
                         */
                        public void setCatNme(String value) {
                            this.catNme = value;
                        }

                    }

                }

            }

        }

    }

}
