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
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>


<div class="yui-b">
  <h2>Faceted Navigation</h2>
        <c:if test="${childNav}">
            <hst:facetnavigationlink var="removeAll" current="${facetnav}" removeList="${facetnav.ancestorsAndSelf}"/>
            Clear all [<a href="${removeAll}" style="color:red">X</a>]
            <br/><br/>
        </c:if> 
        <c:forEach var="facet" items="${facetnav.folders}">
           <ul>               
            <li class="title">
                ${facet.name} (${facet.count})
            </li>
            <c:forEach var="facetvalue" items="${facet.folders}">
	            <div style="margin-left:20px">
	               <c:choose>
	                 <c:when test="${facetvalue.leaf}">
	                    ${facetvalue.name} <b>(${facetvalue.count})</b>
	                    <c:if test="${facetvalue.count > 0}">
	                        <hst:facetnavigationlink var="remove" current="${facetnav}" remove="${facetvalue}"/>
	                        [<a href="${remove}" style="color:red">X</a>]
	                    </c:if>
	                 </c:when>
	                 <c:otherwise>
	              	   <hst:link var="link" hippobean="${facetvalue}"/>
	         	  	   <a href="${link}">${facetvalue.name} <b>(${facetvalue.count})</b></a> 
	                 </c:otherwise>
	               </c:choose>
	            </div>
	        </c:forEach>
          </ul>
   </c:forEach>
</div>
