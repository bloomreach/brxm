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
<%--@elvariable id="queryResponse" type="org.apache.solr.client.solrj.response.QueryResponse"--%>

<div class="yui-b">

  <hst:link var="searchURL"  />
  <form action="${searchURL}" method="get">
    <div>
      <b>Query:</b>
      <input type="text" name="query" value="${query}" />

      <br/>
      <input type="submit" value="Search" />
    </div>

  </form>

  <hst:link var="currentLink"/>

  <c:forEach var="facetField" items="${queryResponse.facetFields}">
    <br/>
    ${facetField.name}<br/>
    <ul style="padding-left:20px;">
      <c:forEach var="facet" items="${facetField.values}">
        <li>
            <c:set var="facetLink" value="${currentLink}/${facet.facetField.name}/${facet.name}" />
            <c:choose>
              <c:when test="${query eq null}">
                <a href="${facetLink}">${facet.name} (${facet.count})</a>
              </c:when>
              <c:otherwise>
                <a href="${facetLink}?query=${query}">${facet.name} (${facet.count})</a>
              </c:otherwise>
            </c:choose>
            
        </li>
      </c:forEach>
    </ul>
  </c:forEach>
  <c:forEach var="facetRange" items="${queryResponse.facetRanges}">
    <br/>
    ${facetRange.name}<br/>
    <ul style="padding-left:20px;">
      <c:forEach var="facet" items="${facetRange.counts}">
        <li>
            ${facet.value} (${facet.count})
        </li>
      </c:forEach>
    </ul>
  </c:forEach>

</div>
