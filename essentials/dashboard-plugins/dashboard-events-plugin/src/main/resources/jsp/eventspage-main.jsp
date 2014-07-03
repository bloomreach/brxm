<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --%>

<%--@elvariable id="document" type="{{beansPackage}}.EventsDocument"--%>

<hst:link var="link" hippobean="${document}"/>
<article class="has-edit-button">
  <hst:cmseditlink hippobean="${document}"/>
  <h3><a href="${link}"><c:out value="${document.title}"/></a></h3>
  <c:if test="${hst:isReadable(document, 'date.time')}">
    <p>
      <fmt:formatDate value="${document.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>
  <c:if test="${hst:isReadable(document, 'enddate.time')}">
    <p>
      <fmt:formatDate value="${document.enddate.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>

  <c:if test="${not empty document.location}">
    <p><c:out value="${document.location}"/></p>
  </c:if>

  <c:if test="${not empty document.introduction}">
    <p><c:out value="${document.introduction}"/></p>
  </c:if>

  <c:if test="${hst:isReadable(document, 'image.original')}">
    <hst:link var="img" hippobean="${document.image.original}"/>
    <figure>
      <img src="${img}" title="${fn:escapeXml(document.image.fileName)}"
           alt="${fn:escapeXml(document.image.fileName)}"/>
      <figcaption>${fn:escapeXml(document.image.description)}</figcaption>
    </figure>
  </c:if>

  <hst:html hippohtml="${document.content}"/>

</article>