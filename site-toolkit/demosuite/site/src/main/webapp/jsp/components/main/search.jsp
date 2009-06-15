<%--
  Copyright 2009 Hippo

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
<div id="yui-u">
    <h1>Search</h1>
        
    <hst:link var="actionURL" path="/search" />
    <form action="${actionURL}" method="post">
      <input type="text" name="query" value="${query}"/>
      <input type="submit" value="Search"/>
    </form>
        
    <c:if test="${query != null && query != ''}">

        <c:choose>
            <c:when test="${searchResults.total > 0}">
		        <p/>        
		        <p>
		        Results <b>${searchResults.startOffset +1} - ${searchResults.endOffset}</b> of <b>${searchResults.total}</b> for <b>${query}</b>.
		        </p>
		        
		        <c:forEach var="bean" items="${searchResults.items}" varStatus="indexer">
		            <hst:link var="link" hippobean="${bean.item}"/>                         
		            <ul class="list-overview">
		                <li class="title">
		                    <a href="${link}">${bean.title}</a>
		                    <div>
		                        <p><fmt:formatDate value="${bean.date.time}" type="Date" pattern="MMMM d, yyyy h:mm a" /></p>
		                        <p>${bean.text}</p>
		                    </div>
		                </li>
		                
		                </li>
		            </ul>
		        </c:forEach>
		        
		        <c:if test="${searchResults.totalPages > 1}">
		        
		            <ul id="paging-nav">
		                <c:forEach var="pageNr" items="${searchResults.pageNumbersArray}" varStatus="status">
		                    <c:set var="active" value="" />
		                    <c:if test="${searchResults.currentPage == pageNr}">
		                          <c:set var="active" value=" class=\"active\"" />
		                    </c:if>
		                    <hst:renderURL var="pagelink">
		                        <hst:param name="page" value="${pageNr}" />
		                        <hst:param name="query" value="${query}" />
		                    </hst:renderURL>
		                    <li${active}><a href="${pagelink}" title="${pageNr}">${pageNr}</a></li>
		                </c:forEach>
		            </ul>
		        
		        </c:if>

            </c:when>
            <c:otherwise>
                <p/>
                <p>No results for <b>${query}</b>.</p>
            </c:otherwise>
        </c:choose>
        
    </c:if>
    
</div>
