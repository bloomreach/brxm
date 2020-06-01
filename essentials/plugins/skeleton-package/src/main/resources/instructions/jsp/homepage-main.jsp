<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<hst:setBundle basename="essentials.homepage"/>
<div>
  <h1><fmt:message key="homepage.title" var="title"/><c:out value="${title}"/></h1>
  <p><fmt:message key="homepage.text" var="text"/><c:out value="${text}"/></p>
  <c:if test="${!hstRequest.requestContext.cmsRequest}">
    <p>
      [This text can be edited <a href="http://localhost:8080/cms/?1&path=/content/documents/administration/labels/homepage" target="_blank">here</a>.]
    </p>
  </c:if>
</div>
<div>
  <hst:include ref="container"/>
</div>