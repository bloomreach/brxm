<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%--@elvariable id="document" type="{{beansPackage}}.Blogpost"--%>
<h1><c:out value="${document.title}"/></h1>
<h2>by: <c:out value="${document.author}"/></h2>
<strong>
  <c:if test="${document.publicationDate ne null}">
    <fmt:formatDate type="date" pattern="yyyy-MM-dd" value="${document.publicationDate.time}"/>
  </c:if>
</strong>
<p><c:out value="${document.introduction}"/></p>
<div><hst:html hippohtml="${document.content}"/></div>