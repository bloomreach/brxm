<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>


<div class="yui-b">
  <h2>Faceted Navigation</h2>
        
        <hst:link var="searchLink" hippobean="${facetnav}"/>
        
        <form method="get" action="${searchLink}"/>
            <input type="text" name="query" value="${query}"/>
        </form>
        
        <c:if test="${childNav}">
            <hst:link var="removeAll" hippobean="${facetnav.rootFacetNavigationBean}"/>
            <p>Clear all [<a href="${removeAll}" class="deleteFacet">X</a>]</p>
        </c:if> 
        <c:forEach var="facet" items="${facetnav.folders}">
           <ul>               
            <li class="title">
                ${facet.name} (${facet.count})
                <c:if test="${not empty facet.folders}">
                    <ul class="facets">
                        <c:forEach var="facetvalue" items="${facet.folders}">
                          <li>
                             <c:choose>
                               <c:when test="${facetvalue.leaf}">
                                  <c:out value="${facetvalue.name}" escapeXml="true"/> <b>(${facetvalue.count})</b>
                                  <c:if test="${facetvalue.count > 0}">
                                      <hst:facetnavigationlink var="remove" current="${facetnav}" remove="${facetvalue}"/>
                                      [<a href="${remove}" class="deleteFacet">X</a>]
                                  </c:if>
                               </c:when>
                               <c:otherwise>
                                 <hst:link var="link" hippobean="${facetvalue}" navigationStateful="true"/>
                                 <a href="${link}"><c:out value="${facetvalue.name}" escapeXml="true"/> <b>(${facetvalue.count})</b></a> 
                               </c:otherwise>
                             </c:choose>
                          </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </li>
          </ul>
   </c:forEach>
</div>
