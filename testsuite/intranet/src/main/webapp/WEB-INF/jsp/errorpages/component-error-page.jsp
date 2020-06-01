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
<%--@elvariable id="errorComponentWindow" type="org.hippoecm.hst.core.container.HstComponentWindow"--%>

<c:if test="${not empty errorComponentWindow.componentExceptions}">
  <ul>
    <c:forEach var="componentException" items="${errorComponentWindow.componentExceptions}">
      <li>
        <pre>${fn:escapeXml(componentException.message)}</pre>
        <!--
            <c:forEach items="${componentException.stackTrace}" var="line">
              ${fn:escapeXml(line)}<br>
            </c:forEach>
          -->
      </li>
    </c:forEach>
  </ul>
</c:if>