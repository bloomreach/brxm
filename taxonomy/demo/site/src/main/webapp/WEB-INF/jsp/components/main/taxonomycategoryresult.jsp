<%--
  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)

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
<%@ page language="java" import="org.onehippo.taxonomy.api.*, org.apache.commons.lang.StringUtils" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<hst:defineObjects/>

<c:set var="categoryInfo" value="${category.infos[hstRequest.locale]}" />

<div>
  <h2>Taxonomy Category: ${category.name} '${hstRequest.locale.toLanguageTag()}'</h2>
    <c:if test="${not empty categoryInfo}">
    <ul>
      <li>Name: <c:out value="${categoryInfo.name}"/></li>
      <li>Description: <pre><c:out value="${categoryInfo.description}"/></pre></li>
      <li>Full Description: <pre><c:out value="${categoryInfo.properties['hippotaxonomy:fulldescription']}"/></pre></li>
      <li>Synonyms: <c:out value="${fn:join(categoryInfo.synonyms, ', ')}"/></li>
    </ul>
    </c:if>

    <hr/>

    <h2>Documents in this taxonomy category:</h2>

    <ul>
      <c:forEach var="bean" items="${documents}">
        <li>
          <hst:link var="link" hippobean="${bean}" />
          <a href="${link}"><c:out value="${bean.title}"/></a>
        </li>
      </c:forEach>
    </ul>

</div>

