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
package com.fis.bic.sso.utility;

/**
 *  Description of the Class
 *
 *@author     Metavante Corp.
 *@created    February 12, 2004
 */
public class NestedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 *  Constructor for the NestedException object
	 */
	public NestedException() { }

	/**
	 *  Constructor for the NestedException object
	 *
	 *@param  s  Description of the Parameter
	 */
	public NestedException(String s) {
		super(s);
	}

	/**
	 *  Constructor for the NestedException object
	 *
	 *@param  throwable  Description of the Parameter
	 */
	public NestedException(Throwable throwable) {
		m_parentException = throwable;
	}

	/**
	 *  Constructor for the NestedException object
	 *
	 *@param  s          Description of the Parameter
	 *@param  throwable  Description of the Parameter
	 */
	public NestedException(String s, Throwable throwable) {
		super(s);
		m_parentException = throwable;
	}

	/**
	 *  Gets the message attribute of the NestedException object
	 *
	 *@return    The message value
	 */
	public String getMessage() {
		String s = super.getMessage();

		if (m_parentException == null)
			return s;
		StringBuffer stringbuffer = new StringBuffer();
		String s1 = m_parentException.getMessage();

		if (s != null)
			stringbuffer.append(s).append(": ").append(s1);
		else
			stringbuffer.append(s1);
		return stringbuffer.toString();
	}

	public String getParentMessage() {
	    return super.getMessage();
	}

	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public Throwable getparentException() {
		return m_parentException;
	}

	/**
	 *  Description of the Field
	 */
	protected Throwable m_parentException;
}
