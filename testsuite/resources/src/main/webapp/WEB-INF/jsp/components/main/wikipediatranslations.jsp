<%--
  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)

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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.hippoecm.hst.content.beans.standard.AvailableTranslations" %>
<%@ page import="org.hippoecm.hst.demo.beans.WikiBean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<%
String[] locales = { "en", "de", "fr", "it", "nl" };
request.setAttribute("locales", locales);
%>

<div id="yui-u">

  <h1>Wikipedia Translations</h1>

  <table border="2">
    <thead>
      <tr>
        <c:forEach var="locale" items="${locales}">
          <th><c:out value="${locale}"/></th>
        </c:forEach>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="wikiBean" items="${result}">
        <c:set var="availableTranslations" value="${wikiBean.availableTranslations}" />
        <%
        AvailableTranslations availableTranslations = (AvailableTranslations) pageContext.getAttribute("availableTranslations");
        %>
        <tr>
          <% for (String locale : locales) { %>
            <td>
              <%
              if (availableTranslations.hasTranslation(locale)) {
                WikiBean translatedWikiBean = (WikiBean) availableTranslations.getTranslation(locale);
                out.println("<h4>" + translatedWikiBean.getTitle() + "</h4>");
                out.println("<div>Categories: " + StringUtils.join(translatedWikiBean.getCategories(), ", ") + "</div>");
              }
              %>
            </td>
          <% } %>
        </tr>
      </c:forEach>
    </tbody>
  </table>

</div>
