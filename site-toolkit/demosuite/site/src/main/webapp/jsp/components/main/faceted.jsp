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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="nextpage" value="${searchResults.next}"/>
<c:set var="offset" value="${(searchResults.currentPage -1) * searchResults.pageSize}"/>
<c:set var="prevpage" value="${searchResults.previous}"/>
<c:set var="firstpage" value="${searchResults.startPage}"/>
<c:set var="lastpage" value="${searchResults.endPage}"/>

<hst:head-contribution keyHint="title"><title>Faceted Navigation Result</title></hst:head-contribution>

<div id="yui-u">
        <h1>Results</h1>
        
        <c:if test="${not empty subnavigation}">
            <c:forEach var="ancestor" items="${subnavigation.ancestors}">
            	<div>
        	    	${ancestor.facetValueCombi.key} = ${ancestor.facetValueCombi.value} 
        	    	<hst:facetnavigationlink var="withoutAncestor" current="${subnavigation}" remove="${ancestor}"/>
        	    	[<a href="${withoutAncestor}" style="color:red">X</a>]
        	    </div>
            </c:forEach>
        	<div>
        		${subnavigation.facetValueCombi.key} = ${subnavigation.facetValueCombi.value} 
				<hst:facetnavigationlink var="withoutAncestor" current="${subnavigation}" remove="${subnavigation}"/>
        	    [<a href="${withoutAncestor}" style="color:red">X</a>]
        	</div>
        </c:if>
        
        <c:choose>
          <c:when test="${empty resultset}">
          	Navigate the faceted tree to see the results here

            <br/>
            <br/>
            <br/>
            Wanna populate some dummy news items to see faceted navigation in real action? <b>node</b> adding moren then, say 1000 news items takes some time.
         <form action="<hst:actionURL/>" method>
            <input type="hidden" name="number" value="5"/>
            <input type="submit" value="add 5 more random cars"/>
         </form>
         <form action="<hst:actionURL/>" method>
             <input type="hidden" name="number" value="25"/>
             <input type="submit" value="add 25 more random cars"/>
         </form>
         <form action="<hst:actionURL/>" method>
             <input type="hidden" name="number" value="100"/>
             <input type="submit" value="add 100 more random cars"/>
         </form>
         <form action="<hst:actionURL/>" method>
             <input type="hidden" name="number" value="250"/>
             <input type="submit" value="add 250 more random cars"/>
         </form>
          </c:when>
          <c:otherwise>
	        <c:forEach var="result" items="${resultset}">
	          <ul class="list-overview">
	            <hst:link var="link" hippobean="${result}">
	            <hst:sitemapitem preferPath="/faceted"/>
	            </hst:link>
	            <li class="title">
	               <c:choose>
			          <c:when test="${empty result.title}">
			        	  <a href="${link}">${result.name}</a>
			          </c:when>
			          <c:otherwise>
			         	 <a href="${link}">${result.title}</a>
			          </c:otherwise>
				   </c:choose>
	            </li>
	          </ul>
	        </c:forEach>
          </c:otherwise>
        </c:choose>
</div>
