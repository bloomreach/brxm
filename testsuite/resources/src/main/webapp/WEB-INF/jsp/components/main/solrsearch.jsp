<%--
  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)

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

  <c:choose>
    <c:when test="${query eq null}">
      No search
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${result.size > 0}">
          <p></p>
          <p><b>${result.size}</b> results for <b>${query}</b>.</p>
          <c:if test="${fn:length(pages) gt 0}">
             <ul id="paging-nav"> <b>pages</b>
              <c:forEach var="page" items="${pages}">
                <c:set var="active" value=""/>
                <c:choose>
                  <c:when test="${crPage == page}">
                    <li>${page}</li>
                  </c:when>
                  <c:otherwise>
                    <hst:renderURL var="pagelink">
                      <hst:param name="page" value="${page}"/>
                    </hst:renderURL>
                    <li><a href="${pagelink}" title="${page}">${page}</a></li>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
            </ul>
          </c:if>
          <p></p>
          <c:forEach var="hit" items="${result.hits}">
            <c:set var="bean" value="${hit.bean}"/>
            <hst:link var="link" hippobean="${bean}"/>
            <ul class="list-overview">
              <li class="title"><b><a href="${link}">${bean.title}</a></b> <c:if test="${hit.score > 0}">(${hit.score})</c:if>
                <div>
                  <c:if test="${hst:isReadable(bean, 'date')}">
                    <p>
                      <fmt:formatDate value="${bean.date.time}" type="Date"
                                      pattern="MMMM d, yyyy h:mm a"/>
                    </p>
                  </c:if>
                  <c:forEach var="highlight" items="${hit.highlights}">
                    <c:forEach var="excerpt" items="${highlight.excerpts}">
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

        </c:when>
        <c:otherwise>
          <p></p>
          <p>No results for <b>${query}</b>.</p>
          <c:if test="${result.queryResponse.spellCheckResponse != null}">
            Did you mean '<b>${result.queryResponse.spellCheckResponse.collatedResult}</b>' ?
            <br/><br/>
            Corrections: <br/>
            <div>
              <c:forEach var="collation" items="${result.queryResponse.spellCheckResponse.collatedResults}" >
                <c:forEach var="correction" items="${collation.misspellingsAndCorrections}">
                  <i>${correction.original}</i> --> <b>${correction.correction}</b><br/>
                </c:forEach>
              </c:forEach>
            </div>

            <br/><br/>
            <p>
              Or did you mean one of these below? <br/>
              <div>
                <c:forEach var="suggestion" items="${result.queryResponse.spellCheckResponse.suggestions}">
                  <i>${suggestion.token}</i><br/>
                  <c:forEach var="alternative" items="${suggestion.alternatives}" varStatus="counter">
                    <c:if test="${counter.index < 7}">
                      &nbsp;&nbsp;&nbsp;${alternative}<br/>
                    </c:if>
                  </c:forEach>
                </c:forEach>
              </div>
            </p>
          </c:if>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>
</div>
