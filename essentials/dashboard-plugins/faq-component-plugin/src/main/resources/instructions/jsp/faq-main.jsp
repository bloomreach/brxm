<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.FaqDocument"--%>
<h1>${document.question}</h1>
<hst:html hippohtml="${document.answer}"/>
