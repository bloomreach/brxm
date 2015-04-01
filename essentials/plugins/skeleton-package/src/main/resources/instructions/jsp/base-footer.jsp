<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<hst:setBundle basename="essentials.global"/>
<hr></hr>
<div class="text-center">
  <sub><fmt:message key="footer.text" var="footer"/><c:out value="${footer}"/></sub>
</div>
<hst:include ref="container"/>
