<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.FaqListDocument"--%>
<h1>${document.title}</h1>
<div><hst:html hippohtml="${document.description}"/></div>
<c:forEach var="faq" items="${document.faqDocuments}">
  <div>
    <h3><a href="<hst:link hippobean="${faq}"/>">${faq.question}</a></h3>
    <hst:html hippohtml="${faq.answer}"/>
  </div>
</c:forEach>
<c:if test="${editMode && (document eq null)}">
  <img src="<hst:link path="/images/essentials-edit-component.png" />" alt="Edit component settings">
</c:if>

