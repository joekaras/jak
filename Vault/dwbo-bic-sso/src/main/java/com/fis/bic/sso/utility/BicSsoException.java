package com.fis.bic.sso.utility;


public class BicSsoException extends NestedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static BicLogger logger = BicLogger.getLogger(BicSsoException.class);
	

    /**
	 * 
	 */

	/**
     * Constructs a new BicSsoException with no description and no parent exception
     *
     * @param errorCode the error code
     * @param type  the type
     */
    public BicSsoException() {
        super();
    }

    /**
     * Constructs a new BicSsoException with the specified
     * description and with no parent exception
     *
     * @param message   description of the exception that has occured.
     */
    public BicSsoException(String message) {    	
        super(message);
        logger.error(message);
     }

    /**
     * Constructs a new BicSsoException with the specified
     * parent exception and with no description
     *
     * @param aParentException   the parent exception.
     */
    public BicSsoException(Throwable aParentException) {
        super(aParentException);
        logger.error(aParentException);
    }

    /**
     * Constructs a new BicSsoException with the specified
     * description and parent exception
     *
     * @param message   description of the exception that has occured.
     * @param aParentException   the parent exception.
     */
    public BicSsoException(String message, Throwable aParentException) {
        super(message, aParentException);
        logger.error(message,aParentException);
    }

}
