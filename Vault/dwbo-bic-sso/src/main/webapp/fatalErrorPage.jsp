<%-- Page Directives --%>
<%@ page language="java" buffer="8kb" autoFlush="true" isErrorPage="true" %>
<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>
<%@ page import="com.fis.bic.sso.utility.BicLogger"%>

	<% 
	BicLogger logger = BicLogger.getLogger(this.getClass());
	%>
	
		<html>		
			<head>
				<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
				<title> SSO Error !! </title>
				
			</head>
			
			<body>
				<p class="error">Single Sign On failed, please contact technical support. </p>
		
			</body>
		</html>

