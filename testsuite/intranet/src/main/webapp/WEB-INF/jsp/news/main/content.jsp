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
<%--@elvariable id="document" type="org.hippoecm.hst.intranet.beans.NewsDocument"--%>

<c:choose>
  <c:when test="${empty document}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>
    <c:if test="${not empty document.title}">
      <hst:element var="headTitle" name="title">
        <c:out value="${document.title}"/>
      </hst:element>
      <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
    </c:if>

    <article class="well well-large">
      <hst:cmseditlink hippobean="${document}"/>
      <header>
        <h2>${fn:escapeXml(document.title)}</h2>
        <c:if test="${hst:isReadable(document, 'date.time')}">
          <p class="badge badge-info">
              <fmt:formatDate value="${document.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
          </p>
        </c:if>
        <p>${fn:escapeXml(document.summary)}</p>
      </header>
      <hst:html hippohtml="${document.html}"/>
      <c:if test="${hst:isReadable(document, 'image.original')}">
        <hst:link var="img" hippobean="${document.image.original}"/>
        <figure>
          <img src="${img}" title="${fn:escapeXml(document.image.fileName)}"
            alt="${fn:escapeXml(document.image.fileName)}"/>
          <figcaption>${fn:escapeXml(document.image.description)}</figcaption>
        </figure>
      </c:if>
    </article>

  </c:otherwise>
</c:choose>