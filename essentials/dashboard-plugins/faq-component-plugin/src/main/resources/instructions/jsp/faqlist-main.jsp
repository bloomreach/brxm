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

<%--@elvariable id="document" type="{{beansPackage}}.FaqListDocument"--%>
<div class="has-edit-button">
<hst:cmseditlink hippobean="${document}"/>
<h1>${document.title}</h1>
<div><hst:html hippohtml="${document.description}"/></div>
<c:forEach var="faq" items="${document.faqDocuments}">
  <div>
    <h3><a href="<hst:link hippobean="${faq}"/>">${faq.question}</a></h3>
    <hst:html hippohtml="${faq.answer}"/>
  </div>
</c:forEach>
<c:if test="${editMode && (document eq null)}">
  <img src="<hst:link path="/images/essentials-edit-component.png" />" alt="Edit component settings">
</c:if>
</div>
