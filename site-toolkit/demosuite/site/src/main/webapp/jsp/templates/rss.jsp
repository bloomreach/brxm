<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<hst:link var="feedlink" path="/rss.xml"/>
<rss version="2.0">
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
         <hst:link hippobean="${item}" var="link"/>

         <title>${item.title}</title>
         <link>http://localhost:8085${link}</link>
         <description>${item.summary}</description>
         <pubDate><fmt:formatDate value="${item.date.time}" pattern="EE, dd MMM yyyy HH:mm:ss Z"/></pubDate>
         <guid>http://localhost:8085${link}</guid>

      </item>
      </c:forEach>
      
   </channel>
</rss>