<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>
  
<hst-tmpl:module name="mainnav" var="navitems" className="org.hippoecm.hst.components.modules.navigation.RepositoryBasedNavigationModule" execute="false" render="true"/>


<c:forEach var="item" items="${navitems}">
  <div id="nav${item.name}">
    <ul>
      <li>${item.decodedName}</li>
    <c:forEach var="subItem" items="${item.children}">
      <ul class="subnav">
        <c:set var="firstSubSubItem" value="${subItem.children[0]}"/>
        <c:choose>
          <c:when test="${firstSubSubItem.path!=null}">
            <li><a href="${pageContext.request.contextPath}${requestScope.currentUrlbase}/page${firstSubSubItem.path}" title="${subItem.decodedName}">${subItem.decodedName}</a></li>
          </c:when>
          <c:otherwise>
            <li><a href="${pageContext.request.contextPath}${requestScope.currentUrlbase}/page${subItem.path}" title="${subItem.decodedName}">${subItem.decodedName}</a></li>
          </c:otherwise>
        </c:choose>        	    
      </ul>
    </c:forEach>  
    </ul>
  </div>		
</c:forEach>					



