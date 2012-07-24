<%--
  Copyright 2008-2010 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@page contentType="application/rss+xml; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<fmt:setLocale value="en-US"/>

<hst:link var="feedlink" path="/rss.xml"/>
<rss version="2.0">
   <fmt:setLocale value="en-US"/>
   <channel>
      <title>HST Demo RSS Feed</title>
      <link>http://localhost:8085${feedlink}</link>
      <description>Latest News!</description>
      <language>en-us</language>
      <pubDate><fmt:formatDate value="${today}" pattern="EE, dd MMM yyyy HH:mm:ss Z"/></pubDate>

      <lastBuildDate><fmt:formatDate value="${today}" pattern="EE, dd MMM yyyy HH:mm:ss Z"/></lastBuildDate>
      <docs>http://blogs.law.harvard.edu/tech/rss</docs>
      <generator>Hippo CMS</generator>
      <managingEditor>editor@example.com</managingEditor>
      <webMaster>webmaster@example.com</webMaster>
      
      <c:forEach var="item" items="${items}">
      <item>
         <hst:link hippobean="${item}" var="link" fullyQualified="true"/>
         <title>${item.title}</title>
         <link>${link}</link>
         <description>${item.summary}</description>
         <pubDate><fmt:formatDate value="${item.date.time}" pattern="EE, dd MMM yyyy HH:mm:ss Z"/></pubDate>
         <guid>${link}</guid>
      </item>
      </c:forEach>
      
   </channel>
</rss>