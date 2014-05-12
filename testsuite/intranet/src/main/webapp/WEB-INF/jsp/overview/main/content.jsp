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
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="crPage" type="java.lang.Integer"--%>
<%--@elvariable id="info" type="${package}.componentsinfo.GeneralListInfo"--%>
<%--@elvariable id="page" type="java.util.Collection<java.lang.Integer>"--%>
<%--@elvariable id="pages" type="java.util.Collection<java.lang.Integer>"--%>
<%--@elvariable id="result" type="org.hippoecm.hst.content.beans.query.HstQueryResult"--%>

<c:choose>
  <c:when test="${empty info}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>
    <c:if test="${not empty info.title}">
      <hst:element var="headTitle" name="title">
        <c:out value="${info.title}"/>
      </hst:element>
      <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
    </c:if>

    <h2>
      ${fn:escapeXml(info.title)}
      <c:if test="${not empty result.totalSize}"> Total results ${result.totalSize}</c:if>
    </h2>

    <c:forEach var="item" items="${result.hippoBeans}">
      <hst:link var="link" hippobean="${item}"/>
      <article class="well well-large">
        <hst:cmseditlink hippobean="${item}"/>
        <h3><a href="${link}">${fn:escapeXml(item.title)}</a></h3>
        <c:if test="${hst:isReadable(item, 'date.time')}">
          <p class="badge badge-info">
            <fmt:formatDate value="${item.date.time}" type="both" dateStyle="medium"
              timeStyle="short"/>
          </p>
        </c:if>
        <p>${fn:escapeXml(item.summary)}</p>
      </article>
    </c:forEach>

    <!--if there are pages on the request, they will be printed by the tag:pages -->
    <tag:pages pages="${pages}" page="${page}"/>

  </c:otherwise>
</c:choose>