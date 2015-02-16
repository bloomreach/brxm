<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.ContentDocument"--%>
<c:if test="${not empty document}">
  <hst:link var="link" hippobean="${document}"/>
  <article class="has-edit-button">
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
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${editMode && empty document}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/simple-content.png'/>"> Click to edit Simple Content
</c:if>
