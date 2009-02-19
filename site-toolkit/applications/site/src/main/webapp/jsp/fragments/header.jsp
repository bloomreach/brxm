<%@ page language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri='/WEB-INF/hst-core.tld' prefix='hc'%>

<%
System.out.println("Console out from header.jsp");
%>

<!-- include title -->

<hc:content path="t" />

<!-- include style -->

<hc:content path="s" />