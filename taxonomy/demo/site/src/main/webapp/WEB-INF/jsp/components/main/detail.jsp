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
<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/taxonomy" prefix='tax'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<hst:defineObjects/>

<div>
  <h2>${document.title}</h2>
  <p>${document.summary}</p>
  <p>
    <hst:html hippohtml="${document.html}" />
  </p>
  <p>

    <hst:link var="taxonomies" path="/taxonomies" />
    <tax:categories var="list" keys="${document.taxonomyKeys}" />
    <c:forEach var="ancestors" items="${list}">
      <ul>
        <c:forEach var="category" items="${ancestors}">
          <li>
            <c:set var="categoryInfo" value="${category.infos[hstRequest.locale]}" />
            <a href="${taxonomies}/${category.taxonomy.name}/${category.path}">${categoryInfo.name}</a>
          </li>
        </c:forEach>
      </ul>
    </c:forEach>
  </p>
</div>

