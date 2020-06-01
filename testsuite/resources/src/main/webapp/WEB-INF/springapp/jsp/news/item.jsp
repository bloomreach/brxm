<%--
  Copyright 2013 Hippo B.V. (http://www.onehippo.com)

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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<script src="<hst:link path='/javascript/simple-io.js'/>" type="text/javascript"></script>
<link id="yuiResetFontsGridCss" rel="stylesheet" type="text/css" href="<hst:link path='/css/yui-2.8.1-reset-fonts-grids.css'/>"/>
<link id="demoSiteCss" rel="stylesheet" type="text/css" href="<hst:link path='/css/style.css'/>" />
<title>Spring MVC Web Application Integration - ${fn:escapeXml(document.title)}</title>
</head>

<body>

  <div id="custom-doc" class="yui-t6">

    <%@ include file="/WEB-INF/springapp/jsp/header.jsp" %>

    <div id="bd">
      <div id="yui-main">
        <div class="yui-b">
          <div class="yui-gf">
            <div class="yui-u first">
              <%@ include file="/WEB-INF/springapp/jsp/left.jsp" %>
            </div>

            <div class="right">
              <a href="<hst:link path='/springapp/news'/>">
                <img src="<hst:link path="/images/goback.jpg"/>" class="noborder" alt="Go back"/>
              </a>
            </div>

            <div id="yui-u">

              <h2>${fn:escapeXml(document.title)}</h2>

              <c:if test="${hst:isReadable(document, 'date.time')}">
                <p class="badge badge-info">
                    <fmt:formatDate value="${document.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
                </p>
              </c:if>

              <p>${fn:escapeXml(document.summary)}</p>

              <div>
                <hst:html hippohtml="${document.html}"/>
              </div>

              <c:if test="${not empty document.resource}">
                <h2>resource link:</h2>
                <hst:link var="resource" hippobean="${document.resource}" />
                <a href="${resource}">${document.resource.name}</a>
                <br/><br/>
              </c:if>

              <c:if test="${not empty document.image}">
                <img src="<hst:link hippobean="${document.image.original}"/>"/>
              </c:if>

            </div>
          </div>
        </div>
      </div>
    </div>

    <%@ include file="/WEB-INF/springapp/jsp/footer.jsp" %>

  </div>

</body>
</html>
