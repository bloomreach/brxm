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
<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>

<%@ attribute name="siteMenuItem" type="org.hippoecm.hst.core.sitemenu.HstSiteMenuItem" rtexprvalue="true"
              required="true" %>

<c:choose>
    <c:when test="${empty siteMenuItem.externalLink}">
        <hst:link var="link" link="${siteMenuItem.hstLink}"/>
    </c:when>
    <c:otherwise>
        <c:set var="link" value="${fn:escapeXml(siteMenuItem.externalLink)}"/>
    </c:otherwise>
</c:choose>

<li ${siteMenuItem.expanded ? 'class="active"' : ''}>
    <a href="${link}">${fn:escapeXml(siteMenuItem.name)}</a>
</li>
<c:if test="${siteMenuItem.expanded and not empty siteMenuItem.childMenuItems}">
    <li>
        <ul class="nav nav-pills nav-stacked">
            <c:forEach var="child" items="${siteMenuItem.childMenuItems}">
                <tag:menuitem siteMenuItem="${child}"/>
            </c:forEach>
        </ul>
    </li>
</c:if>
