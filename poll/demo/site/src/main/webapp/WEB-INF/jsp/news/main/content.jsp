<%--
  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)

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
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="document" type="org.onehippo.forge.polldemo.beans.NewsDocument"--%>

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
    
    <hst:cmseditlink hippobean="${document}"/>
    <h2>${document.title}</h2>
    <c:if test="${hst:isReadable(document, 'date.time')}">
      <p><fmt:formatDate value="${document.date.time}" type="Date"/></p>
    </c:if>
    <p>${document.summary}</p>
    <hst:html hippohtml="${document.html}"/>
    <c:if test="${hst:isReadable(document, 'image.original')}">
      <hst:link var="img" hippobean="${document.image.original}"/>
      <p>
        <br/>
        <img src="${img}" title="${document.image.fileName}"
          alt="${document.image.fileName}"/>
        <br/>
        ${document.image.description}
      </p>
    </c:if>
  </c:otherwise>
</c:choose>