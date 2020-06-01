<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<hst:setBundle basename="essentials.global"/>
<div>
  <hst:include ref="container"/>
</div>
<hr/>
<div class="text-center">
  <sub><fmt:message key="footer.text" var="footer"/><c:out value="${footer}"/></sub>
</div>
