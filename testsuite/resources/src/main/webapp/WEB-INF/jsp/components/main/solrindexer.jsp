<%--
  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<hst:headContribution keyHint="title"><title>Index ALL JCR Documents</title></hst:headContribution>

<hst:defineObjects/>
<div id="yui-u">
    <c:choose>
      <c:when test="${hstRequest.requestContext.preview}">
       <h1>Indexing JCR Documents is only possible in LIVE </h1>
      </c:when>
      <c:otherwise>

        <h1>Index JCR Documents for current site</h1>

        <c:if test="${not empty message}">
          <h2>${message}</h2>
        </c:if>

        <c:forEach var="bean" items="${result.hippoBeans}"
                   varStatus="indexer">
          <hst:link var="link" hippobean="${bean}" />
          <ul class="list-overview">
            <li class="title">
              ${bean.title}
                <form action="<hst:actionURL/>" method="POST">
                  <input type="hidden" name="uuid" value="${bean.canonicalHandleUUID}"/>
                  <input type="submit" value="INDEX"/>
                </form>
            </li>
          </ul>
        </c:forEach>
        
        <form action="<hst:actionURL/>" method="post">
          <input type="submit" value="index"/>
        </form>

      </c:otherwise>
    </c:choose>

</div>
