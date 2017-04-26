<%--
  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
--%>
<%--
  Simple ResourceDataCache invalidation example for a resource space.
  This simple JSP page reads two post request parameters, "resource_space" and "secret",
  and clears the associated ResourceDataCache specified by the "resource_space" parameter value.
  For simplicity, this page compares the "secret" parameter with a hard-coded secret string for security.
  In practice, you will probably want to keep the secret in a different store for security, maintainability, etc.
--%>
<%@ page language="java" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="com.onehippo.cms7.crisp.api.broker.ResourceServiceBroker" %>
<%@ page import="com.onehippo.cms7.crisp.api.resource.ResourceDataCache" %>
<%@ page import="com.onehippo.cms7.crisp.hst.module.CrispHstServices" %>

<%!
private static final String DEFAULT_SECRET = "some_secret";

private static Logger log = LoggerFactory.getLogger("com.onehippo.cms7.crisp.demo.jsp.invalidate-cache");
%>

<%
if (!"POST".equals(request.getMethod())) {
    log.error("Must be a POST request.");
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    return;
}

String resourceSpace = request.getParameter("resource_space");
String secret = request.getParameter("secret");

if (resourceSpace == null || "".equals(resourceSpace.trim())) {
    log.error("Resource space name is missing.");
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    return;
}

if (!DEFAULT_SECRET.equals(secret)) {
    log.error("Wrong secret.");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return;
}

ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker();

if (broker == null) {
    log.error("CRISP was not initialized.");
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return;
}

ResourceDataCache resourceDataCache = broker.getResourceDataCache(resourceSpace);

if (resourceDataCache == null) {
    log.error("No resource data cache for the resource space.");
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return;
}

log.info("Resource data cache cleared for resource space: '{}'", resourceSpace);
resourceDataCache.clear();
out.println("Resource data cache cleared for resource space: '" + resourceSpace + "'.");
%>
