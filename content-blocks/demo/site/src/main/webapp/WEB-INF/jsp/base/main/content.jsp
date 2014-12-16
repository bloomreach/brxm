<%--
  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)

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
<%--@elvariable id="document" type="org.onehippo.forge.contentblocksdemo.beans.ProviderCompoundDocument"--%>

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
      </header>

      <c:if test="${not empty document.contentBlocks}">
        <c:forEach var="item" items="${document.contentBlocks}">
          <c:if test="${not empty item}">
            <c:choose>
              <c:when test="${item.type=='text'}">
                <hst:html hippohtml="${item.text}"/>
              </c:when>
              <c:when test="${item.type=='image'}">
                  <c:if test="${hst:isReadable(item, 'image.original')}">
                    <hst:link var="img" hippobean="${item.image.original}"/>
                    <figure>
                      <img src="${img}" title="${fn:escapeXml(item.image.fileName)}"
                           alt="${fn:escapeXml(item.image.fileName)}"/>
                      <figcaption>${fn:escapeXml(item.image.description)}</figcaption>
                    </figure>
                  </c:if>
                </p>
              </c:when>
              <c:when test="${item.type=='video'}">
                <iframe width="560" height="315" src="//www.youtube.com/embed/${item.video}" frameborder="0" allowfullscreen></iframe>
              </c:when>
            </c:choose>
          </c:if>
        </c:forEach>
      </c:if>
    </article>
  </c:otherwise>
</c:choose>