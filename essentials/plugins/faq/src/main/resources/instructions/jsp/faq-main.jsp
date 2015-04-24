<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.FaqItem"--%>
<div class="has-edit-button">
<hst:cmseditlink hippobean="${document}"/>
<h1><c:out value="${document.question}"/></h1>
<hst:html hippohtml="${document.answer}"/>
</div>
