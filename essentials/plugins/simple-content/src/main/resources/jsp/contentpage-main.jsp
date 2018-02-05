<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.ContentDocument"--%>
<c:if test="${not empty requestScope.document}">
  <hst:link var="link" hippobean="${requestScope.document}"/>
  <article class="has-edit-button">
    <hst:manageContent hippobean="${requestScope.document}"/>
    <h3><a href="${link}"><c:out value="${requestScope.document.title}"/></a></h3>
    <c:if test="${hst:isReadable(requestScope.document, 'publicationdate.time')}">
      <p>
        <fmt:formatDate value="${requestScope.document.publicationDate.time}" type="both" dateStyle="medium" timeStyle="short"/>
      </p>
    </c:if>

    <c:if test="${not empty requestScope.document.introduction}">
      <p><c:out value="${requestScope.document.introduction}"/></p>
    </c:if>
    <hst:html hippohtml="${requestScope.document.content}"/>
  </article>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode && empty requestScope.document}">
  <div class="has-edit-button">
    <img src="<hst:link path='/images/essentials/catalog-component-icons/simple-content.png'/>"> Click to edit Simple Content
    <hst:manageContent templateQuery="new-content-document" rootPath="content"/>
  </div>
</c:if>
