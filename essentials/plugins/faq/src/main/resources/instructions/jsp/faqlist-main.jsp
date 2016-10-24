<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.FaqList"--%>
<c:if test="${requestScope.document ne null}">
  <div class="has-edit-button">
    <c:choose>
      <c:when test="${hst:isReadable(requestScope.document, 'FAQ')}">
        <hst:cmseditlink hippobean="${requestScope.document}"/>
        <h1><c:out value="${requestScope.document.title}"/></h1>
        <div><hst:html hippohtml="${requestScope.document.description}"/></div>
        <c:forEach var="faq" items="${requestScope.document.faqItems}">
          <div>
            <h3><a href="<hst:link hippobean="${faq}"/>"><c:out value="${faq.question}"/></a></h3>
            <hst:html hippohtml="${faq.answer}"/>
          </div>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <div class="alert alert-danger">The selected document should be of type FAQ list.</div>
      </c:otherwise>
    </c:choose>
  </div>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode && (requestScope.document eq null)}">
  <div>
    <img src="<hst:link path='/images/essentials/catalog-component-icons/faq.png'/>"> Click to edit FAQ
  </div>
</c:if>
