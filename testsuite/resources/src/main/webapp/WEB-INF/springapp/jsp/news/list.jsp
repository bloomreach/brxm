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
<title>Spring MVC Web Application Integration - News</title>
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
            <div id="yui-u">

              <h1>Spring MVC Web Application Integration - News</h1>

              <h2><b><c:out value="${totalSize}"/></b> results</h2>

              <c:forEach var="document" items="${documents}">
                <ul class="list-overview-nomargin">
                  <li class="title">
                    <c:set var="relNewsDocPath" value="${fn:substringAfter(document.path, scope.path)}" />
                    <c:url var="link" value="/springapp/news${relNewsDocPath}" />
                    <a href="${link}"><c:out value="${document.title}"/></a>
                    <div>
                      <p><fmt:formatDate value="${document.date.time}" type="Date" pattern="MMMM d, yyyy h:mm a" /></p>
                      <p><c:out value="${document.summary}"/></p>
                    </div>
                  </li>
                </ul>
              </c:forEach>

              <c:if test="${fn:length(pageNums) gt 0}">
                <ul id="paging-nav-nomargin">
                  <c:forEach var="pageNum" items="${pageNums}">
                    <c:set var="active" value="" />
                    <c:choose>
                      <c:when test="${pageIndex == pageNum}">
                        <li>${pageNum}</li>
                      </c:when>
                      <c:otherwise>
                        <hst:link var="pagelink" path="/springapp/news">
                          <hst:param name="pi" value="${pageNum}" />
                        </hst:link>
                        <li><a href="${pagelink}" title="${pageNum}">${pageNum}</a></li>
                      </c:otherwise>
                    </c:choose>
                  </c:forEach>
                </ul>
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