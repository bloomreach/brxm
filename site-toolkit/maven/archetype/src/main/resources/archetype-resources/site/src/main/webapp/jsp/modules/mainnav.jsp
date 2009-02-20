<%--
  Copyright 2008 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>
  
<hst-tmpl:module name="mainnav" var="navitems" className="org.hippoecm.hst.components.modules.navigation.RepositoryBasedNavigationModule" execute="false" render="true"/>
<h3 class="nav_title">Example menu</h3>
<!--  Iterate over all the nodes returned by the above module -->
<c:forEach var="item" items="${dollar}{navitems}">
  <div id="nav${dollar}{item.uuid}">
    <ul>
      <!-- print out the decoded name of the current jcrNode -->
      <li>${dollar}{item.decodedName}
      <c:forEach var="subItem" items="${dollar}{item.children}">
      <ul class="subnav">
        <c:set var="firstSubSubItem" value="${dollar}{subItem.children[0]}"/>
	    <hst-tmpl:link var="link" item="${dollar}{subItem}" />
         <li><a href="${dollar}{link.href}" title="${dollar}{subItem.decodedName}">${dollar}{subItem.decodedName}</a></li>
      </ul>
    </c:forEach>  
    </li>
    </ul>
  </div>		
</c:forEach>					



