<%@ page language="java" contentType="image/jpeg" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ page import="org.onehippo.cms7.crisp.api.resource.Binary" %>
<%@ page import="org.apache.commons.io.IOUtils" %>

<%
Binary binary = null;

try {
    binary = (Binary) request.getAttribute("binary");
    IOUtils.copy(binary.getInputStream(), response.getOutputStream());
} catch (Exception e) {
    e.printStackTrace();
} finally {
    if (binary != null) {
        binary.dispose();
    }
}
%>