<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>
  
<hst-tmpl:module name="mainnav" var="navitems" className="org.hippoecm.hst.components.modules.navigation.RepositoryBasedNavigationModule" execute="false" render="true"/>


<c:forEach var="item" items="${dollar}{navitems}">
  <div id="nav${dollar}{item.name}">
    <ul>
      <li>${dollar}{item.decodedName}</li>
    <c:forEach var="subItem" items="${dollar}{item.children}">
      <ul class="subnav">
        <c:set var="firstSubSubItem" value="${dollar}{subItem.children[0]}"/>
        <c:choose>
          <c:when test="${dollar}{firstSubSubItem!=null}">
	    <hst-tmpl:link var="link" item="${dollar}{firstSubSubItem}" />
            <li><a href="${dollar}{link.href}" title="${dollar}{subItem.decodedName}">${dollar}{subItem.decodedName}</a></li>
          </c:when>
          <c:otherwise>
	    <hst-tmpl:link var="link" item="${dollar}{subItem}" />
            <li><a href="${dollar}{link.href}" title="${dollar}{subItem.decodedName}">${dollar}{subItem.decodedName}</a></li>
          </c:otherwise>
        </c:choose>        	    
      </ul>
    </c:forEach>  
    </ul>
  </div>		
</c:forEach>					



