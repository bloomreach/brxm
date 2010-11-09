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

<hst:head-contribution keyHint="title"><title>Archive</title></hst:head-contribution>

<div id="yui-u">
        <h1>Archive</h1>
        <h2>Displaying ${searchResults.startOffset +1} - ${searchResults.endOffset} of ${searchResults.total} results</h2>
        
        <c:forEach var="bean" items="${searchResults.items}" varStatus="indexer">
          <ul class="list-overview">
            <hst:link var="link" hippobean="${bean.item}"/>                         
            <li class="title">
                <a href="${link}">${bean.title}</a>
                <div>
                    <p><fmt:formatDate value="${bean.date.time}" type="Date" pattern="MMMM d, yyyy h:mm a" /></p>
                    <p>${bean.text}</p>
                </div>
            </li>
          </ul>
        </c:forEach>
        <c:if test="${not empty searchResults.pageNumbersArray}">
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
    
</div>
