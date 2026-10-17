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
 *         &lt;element name="MtvnSvcVer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="MsgUUID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Svc" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="SvcParms">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="ApplID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="SvcID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="SvcVer" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="RqstUUID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                   &lt;element name="MsgData">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;choice>
 *                               &lt;element name="DefaultResData" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                               &lt;element ref="{mtvnETRetrieveUserApplCatAccessResData}RetrieveUserApplCatAccessResponse"/>
 *                             &lt;/choice>
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
    "mtvnSvcVer",
    "msgUUID",
    "svc",
    "errCde",
    "errMsg"
})
@XmlRootElement(name = "ETRetrieveUserApplCatAccessMtvnSvcRes", namespace = "mtvnMBUDSvcRes")
public class ETRetrieveUserApplCatAccessMtvnSvcRes {

    @XmlElement(name = "MtvnSvcVer", namespace = "mtvnMBUDSvcRes")
    protected String mtvnSvcVer;
    @XmlElement(name = "MsgUUID", namespace = "mtvnMBUDSvcRes", required = true)
    protected String msgUUID;
    @XmlElement(name = "Svc", namespace = "mtvnMBUDSvcRes", required = true)
    protected List<ETRetrieveUserApplCatAccessMtvnSvcRes.Svc> svc;
    @XmlElement(name = "ErrCde", namespace = "mtvnMBUDSvcRes", required = true)
    protected String errCde;
    @XmlElement(name = "ErrMsg", namespace = "mtvnMBUDSvcRes")
    protected String errMsg;

    /**
     * Gets the value of the mtvnSvcVer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMtvnSvcVer() {
        return mtvnSvcVer;
    }

    /**
     * Sets the value of the mtvnSvcVer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMtvnSvcVer(String value) {
        this.mtvnSvcVer = value;
    }

    /**
     * Gets the value of the msgUUID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMsgUUID() {
        return msgUUID;
    }

    /**
     * Sets the value of the msgUUID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMsgUUID(String value) {
        this.msgUUID = value;
    }

    /**
     * Gets the value of the svc property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the svc property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSvc().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc }
     * 
     * 
     */
    public List<ETRetrieveUserApplCatAccessMtvnSvcRes.Svc> getSvc() {
        if (svc == null) {
            svc = new ArrayList<ETRetrieveUserApplCatAccessMtvnSvcRes.Svc>();
        }
        return this.svc;
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
     *         &lt;element name="SvcParms">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="ApplID" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                   &lt;element name="SvcID" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                   &lt;element name="SvcVer" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                   &lt;element name="RqstUUID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *                 &lt;/sequence>
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *         &lt;element name="MsgData">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;choice>
     *                     &lt;element name="DefaultResData" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                     &lt;element ref="{mtvnETRetrieveUserApplCatAccessResData}RetrieveUserApplCatAccessResponse"/>
     *                   &lt;/choice>
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
        "svcParms",
        "msgData",
        "errCde",
        "errMsg"
    })
    public static class Svc {

        @XmlElement(name = "SvcParms", namespace = "mtvnMBUDSvcRes", required = true)
        protected ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms svcParms;
        @XmlElement(name = "MsgData", namespace = "mtvnMBUDSvcRes", required = true)
        protected ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData msgData;
        @XmlElement(name = "ErrCde", namespace = "mtvnMBUDSvcRes", required = true)
        protected String errCde;
        @XmlElement(name = "ErrMsg", namespace = "mtvnMBUDSvcRes")
        protected String errMsg;

        /**
         * Gets the value of the svcParms property.
         * 
         * @return
         *     possible object is
         *     {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms }
         *     
         */
        public ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms getSvcParms() {
            return svcParms;
        }

        /**
         * Sets the value of the svcParms property.
         * 
         * @param value
         *     allowed object is
         *     {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms }
         *     
         */
        public void setSvcParms(ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.SvcParms value) {
            this.svcParms = value;
        }

        /**
         * Gets the value of the msgData property.
         * 
         * @return
         *     possible object is
         *     {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData }
         *     
         */
        public ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData getMsgData() {
            return msgData;
        }

        /**
         * Sets the value of the msgData property.
         * 
         * @param value
         *     allowed object is
         *     {@link ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData }
         *     
         */
        public void setMsgData(ETRetrieveUserApplCatAccessMtvnSvcRes.Svc.MsgData value) {
            this.msgData = value;
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
         *         &lt;choice>
         *           &lt;element name="DefaultResData" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *           &lt;element ref="{mtvnETRetrieveUserApplCatAccessResData}RetrieveUserApplCatAccessResponse"/>
         *         &lt;/choice>
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
            "defaultResData",
            "retrieveUserApplCatAccessResponse"
        })
        public static class MsgData {

            @XmlElement(name = "DefaultResData", namespace = "mtvnMBUDSvcRes")
            protected String defaultResData;
            @XmlElement(name = "RetrieveUserApplCatAccessResponse", namespace = "mtvnETRetrieveUserApplCatAccessResData")
            protected RetrieveUserApplCatAccessResponse retrieveUserApplCatAccessResponse;

            /**
             * Gets the value of the defaultResData property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getDefaultResData() {
                return defaultResData;
            }

            /**
             * Sets the value of the defaultResData property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setDefaultResData(String value) {
                this.defaultResData = value;
            }

            /**
             * Gets the value of the retrieveUserApplCatAccessResponse property.
             * 
             * @return
             *     possible object is
             *     {@link RetrieveUserApplCatAccessResponse }
             *     
             */
            public RetrieveUserApplCatAccessResponse getRetrieveUserApplCatAccessResponse() {
                return retrieveUserApplCatAccessResponse;
            }

            /**
             * Sets the value of the retrieveUserApplCatAccessResponse property.
             * 
             * @param value
             *     allowed object is
             *     {@link RetrieveUserApplCatAccessResponse }
             *     
             */
            public void setRetrieveUserApplCatAccessResponse(RetrieveUserApplCatAccessResponse value) {
                this.retrieveUserApplCatAccessResponse = value;
            }

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
         *         &lt;element name="ApplID" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *         &lt;element name="SvcID" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *         &lt;element name="SvcVer" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *         &lt;element name="RqstUUID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
            "applID",
            "svcID",
            "svcVer",
            "rqstUUID"
        })
        public static class SvcParms {

            @XmlElement(name = "ApplID", namespace = "mtvnMBUDSvcRes", required = true)
            protected String applID;
            @XmlElement(name = "SvcID", namespace = "mtvnMBUDSvcRes", required = true)
            protected String svcID;
            @XmlElement(name = "SvcVer", namespace = "mtvnMBUDSvcRes", required = true)
            protected String svcVer;
            @XmlElement(name = "RqstUUID", namespace = "mtvnMBUDSvcRes")
            protected String rqstUUID;

            /**
             * Gets the value of the applID property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getApplID() {
                return applID;
            }

            /**
             * Sets the value of the applID property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setApplID(String value) {
                this.applID = value;
            }

            /**
             * Gets the value of the svcID property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getSvcID() {
                return svcID;
            }

            /**
             * Sets the value of the svcID property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setSvcID(String value) {
                this.svcID = value;
            }

            /**
             * Gets the value of the svcVer property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getSvcVer() {
                return svcVer;
            }

            /**
             * Sets the value of the svcVer property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setSvcVer(String value) {
                this.svcVer = value;
            }

            /**
             * Gets the value of the rqstUUID property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getRqstUUID() {
                return rqstUUID;
            }

            /**
             * Sets the value of the rqstUUID property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setRqstUUID(String value) {
                this.rqstUUID = value;
            }

        }

    }

}
