package com.fis.bic.sso.utility;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public  class BicLogger {

	Logger logger = null;
	private final String name;

	public BicLogger(final String name) {
		this.name = name;
	}

	public BicLogger(Class<?> clazz) {
		this.name = clazz.getName();
		this.logger = LogManager.getLogger(clazz);
	}

	public static BicLogger getLogger(Class<?> clazz) {
		
		return new BicLogger(clazz);
	}

	public static BicLogger getLogger(final String name) {
		return new BicLogger(name);
	}

	public String getName() {
		return name;
	}

	public void fatal(Object message) {

		logger.fatal(message);
	}

	public void fatal(Object message, Object message1) {
		logger.fatal(message + " : " + message1);
	}

	public void fatal(Object message, Throwable throwe) {
		logger.fatal(message, throwe);
	}

	public void error(Object message) {
		logger.error(message);
	}

	public void error(Object message, Object message1) {
		logger.error(message + " : " + message1);
	}

	public void error(Object message, Throwable throwe) {
		logger.error(message, throwe);
	}

	public void warn(Object message) {
		logger.warn(message);
	}

	public void warn(Object message, Object message1) {
		logger.warn(message + " : " + message1);
	}

	public void warn(Object message, Throwable throwe) {
		logger.warn(message, throwe);
	}

	public void info(Object message) {
		logger.info(message);
	}

	public void info(Object message, Object message1) {
		logger.info(message + " : " + message1);
	}

	public void info(Object message, Throwable throwe) {
		logger.info(message, throwe);
	}

	public void debug(Object message) {
		logger.debug(message);
	}

	public void debug(Object message, Object message1) {
		logger.debug(message + " : " + message1);
	}

	public void debug(Object message, Throwable throwe) {
		logger.debug(message, throwe);
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();

	}

	/*
	 * private static PatternLayout myLayout = new PatternLayout(
	 * "%-4r [%t] %-5p %c %x - %m%n"); // Read from INI file private static
	 * boolean enableWindowsEventLogging; static final Logger log4jLogger =
	 * Logger.getLogger(BicLogger.class); private static NTEventLogAppender
	 * eventLogAppender = new NTEventLogAppender(); private static
	 * RollingFileAppender rollingFileAppender;
	 * 
	 * public void logappending() { if (enableWindowsEventLogging) { switch
	 * (Integer.valueOf(Properties.getProperty("warningLevel"))) { case 1:
	 * log4jLogger.setLevel(Level.ALL); break; case 2:
	 * log4jLogger.setLevel(Level.FATAL); break; case 3:
	 * log4jLogger.setLevel(Level.ERROR); break; case 4:
	 * log4jLogger.setLevel(Level.WARN); break; case 5:
	 * log4jLogger.setLevel(Level.INFO); break; case 6:
	 * log4jLogger.setLevel(Level.DEBUG); break; } ; // Event viewer logging
	 * log4jLogger.addAppender(eventLogAppender); } else {
	 * log4jLogger.setLevel(Level.OFF); }
	 * 
	 * rollingFileAppender = new RollingFileAppender(myLayout,
	 * "\\logs\\log4joutput.log", true); }
	 */
}
