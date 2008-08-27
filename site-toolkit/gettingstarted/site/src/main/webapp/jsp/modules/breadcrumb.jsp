<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="org.hippoecm.hst.core.template.*, org.hippoecm.hst.core.template.node.*,javax.jcr.*,java.util.*" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>
  
<hst-tmpl:module name="breadcrumb" var="result" className="org.hippoecm.hst.components.modules.breadcrumb.RepositoryBasedBreadcrumbModule" execute="false" render="true"/>

<div class="breadcrumb">
<a href="/" title="Home">Home</a> &#62;
<c:forEach var="item" items="${result}" varStatus="resultStatus">
  <c:choose>
    <c:when test="${resultStatus.last == true}">
      <span>${item.decodedName}</span>
    </c:when>
    <c:otherwise>
      <a href="${pageContext.request.contextPath}${requestScope.currentUrlbase}/page${item.path}" title="${item.decodedName}">${item.decodedName}</a> &#62;
    </c:otherwise>
  </c:choose>   
</c:forEach>

<hr class="hidden" />
<!--// breadcrumb -->
