<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ page contentType="text/html; charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<h2>My todo list</h2>

<hst:actionURL var="documentActionUrl" />

<c:choose>
  <c:when test="${not empty todoList}">
    <ul class="todo">
      <c:forEach var="item" items="${todoList}">
        <li class="todo">
          <a href="<hst:link hippobean="${item.document}"/>">${item.document.title}</a> (Requested by ${item.requestUsername})
          <div>
            <c:choose>
              <c:when test="${item.type == 'publish'}">
                <form method="post" action="${documentActionUrl}">
                  <div>
	                  Your action:
	                  <input type="hidden" name="requestPath" value="${item.path}" />
	                  <input type="hidden" name="requestType" value="${item.type}" />
	                  <input type="submit" name="documentAction" value="Accept" />
	                  <input type="submit" name="documentAction" value="Reject" />
                  </div>
                </form>
              </c:when>
            </c:choose>
          </div>
        </li>
      </c:forEach>
    </ul>
  </c:when>
  <c:otherwise>
    <i>There's no item now.</i>
  </c:otherwise>
</c:choose>