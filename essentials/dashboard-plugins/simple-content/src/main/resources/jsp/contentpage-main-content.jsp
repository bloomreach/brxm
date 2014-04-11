<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="document" type="{{beansPackage}}.ContentDocument"--%>

<hst:link var="link" hippobean="${document}"/>
<article>
  <hst:cmseditlink hippobean="${document}"/>
  <h3><a href="${link}"><c:out value="${document.title}"/></a></h3>
  <c:if test="${hst:isReadable(document, 'publicationdate.time')}">
    <p>
      <fmt:formatDate value="${document.publicationDate.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>

  <c:if test="${not empty document.introduction}">
    <p><c:out value="${document.introduction}"/></p>
  </c:if>
  <hst:html hippohtml="${document.content}"/>
</article>