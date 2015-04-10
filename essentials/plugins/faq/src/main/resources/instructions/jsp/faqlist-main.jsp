<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.FaqListDocument"--%>
<c:if test="${document ne null}">
  <div class="has-edit-button">
    <c:choose>
      <c:when test="${hst:isReadable(document, 'FAQ')}">
        <hst:cmseditlink hippobean="${document}"/>
        <h1><c:out value="${document.title}"/></h1>
        <div><hst:html hippohtml="${document.description}"/></div>
        <c:forEach var="faq" items="${document.faqDocuments}">
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
<c:if test="${editMode && (document eq null)}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/faq.png'/>"> Click to edit FAQ
</c:if>
