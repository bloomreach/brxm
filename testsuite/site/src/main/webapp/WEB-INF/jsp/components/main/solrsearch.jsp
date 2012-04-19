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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="result" type="org.hippoecm.hst.solr.content.beans.query.HippoQueryResult"--%>



<div id="yui-u">
   <hst:link var="searchURL" path="/solrSearch" />
   <form action="${searchURL}" method="get">
      <div><input type="text" name="query" value="${query}" /> <input
        type="submit" value="Search" /> 
      </div>
   </form>

   <c:if test="${query != null && query != ''}">
  
    <c:choose>
      <c:when test="${result.size > 0}">
        <p></p>
        <p><b>${result.size}</b> results for <b>${query}</b>.</p>
  
        <c:forEach var="hit" items="${result.hits}">
          <c:set var="bean" value="${hit.contentBean}"/>
          <hst:link var="link" hippobean="${bean}" />
          <ul class="list-overview">
            <li class="title"><a href="${link}">${bean.title}</a> (${hit.score})
              <div>
                <c:if test="${hst:isReadable(bean, 'date')}">
                  <p>
                    <fmt:formatDate value="${bean.date.time}" type="Date"
                                    pattern="MMMM d, yyyy h:mm a"/>
                  </p>
                </c:if>
                 <c:forEach var="highlight" items="${hit.highlights}" >
                  <c:forEach var="excerpt" items="${highlight.excerpts}" >
                    <p>
                        ${excerpt}
                    </p>
                  </c:forEach>
                 </c:forEach>
                <%--c:if test="${hst:isReadable(bean, 'summary')}">
                  <p>${bean.summary}</p>
                </c:if--%>
              </div>
            </li>
          </ul>
        </c:forEach>
  
        <c:if test="${fn:length(pages) gt 0}">
          <ul id="paging-nav">
            <c:forEach var="page" items="${pages}">
              <c:set var="active" value="" />
              <c:choose>
                <c:when test="${crPage == page}">
                  <li>${page}</li>
                </c:when>
                <c:otherwise>
                  <hst:renderURL var="pagelink">
                    <hst:param name="page" value="${page}" />
                    <hst:param name="query" value="${query}" />
                  </hst:renderURL>
                  <li><a href="${pagelink}" title="${page}">${page}</a></li>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </ul>
        </c:if>
  
      </c:when>
      <c:otherwise>
        <p></p>
        <p>No results for <b>${query}</b>.</p>
      </c:otherwise>
    </c:choose>
  
  </c:if>
</div>
