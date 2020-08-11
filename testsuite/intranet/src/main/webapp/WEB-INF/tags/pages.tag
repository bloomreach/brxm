<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<%@ attribute name="pages" required="true" type="java.util.List" rtexprvalue="true" %>
<%@ attribute name="page" required="true" type="java.lang.Integer" rtexprvalue="true" %>
<c:if test="${not empty pages}">
  <div class="pagination">
    <ul>
      <c:forEach var="p" items="${pages}">
        <c:set var="active" value=""/>
        <c:choose>
          <c:when test="${page == p}">
            <li class="active"><a href="#">${page}</a></li>
          </c:when>
          <c:otherwise>
            <hst:renderURL var="pagelink">
              <hst:param name="page" value="${p}"/>
            </hst:renderURL>
            <li><a href="${pagelink}" title="${p}">${p}</a></li>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </ul>
  </div>
</c:if>