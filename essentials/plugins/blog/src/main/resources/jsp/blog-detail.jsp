<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.Blogpost"--%>
<div class="has-edit-button">
  <hst:manageContent hippobean="${requestScope.document}"/>
  <h1><c:out value="${requestScope.document.title}"/></h1>
  <h2>by: <c:out value="${requestScope.document.author}"/></h2>
  <strong>
    <c:if test="${requestScope.document.publicationDate ne null}">
      <fmt:formatDate type="date" pattern="yyyy-MM-dd" value="${requestScope.document.publicationDate.time}"/>
    </c:if>
  </strong>
  <p><c:out value="${requestScope.document.introduction}"/></p>
  <div><hst:html hippohtml="${requestScope.document.content}"/></div>
</div>