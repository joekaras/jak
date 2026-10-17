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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RetrieveUserApplCatAccessRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RetrieveUserApplCatAccessRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PrcsGrpId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RetrieveUserApplCatAccessRequestType", propOrder = {
    "prcsGrpId"
})
public class RetrieveUserApplCatAccessRequestType {

    @XmlElement(name = "PrcsGrpId", required = true)
    protected String prcsGrpId;

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

}
