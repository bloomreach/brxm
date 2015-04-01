<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<hst:setBundle basename="essentials.homepage"/>
<div>
  <h1><fmt:message key="homepage.title" var="title"/><c:out value="${title}"/></h1>
  <p><fmt:message key="homepage.text"/><%--Skip XML escaping--%></p>
</div>
<hst:include ref="container"/>
