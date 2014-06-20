<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.FaqDocument"--%>
<h3>${document.question}</h3>
<hst:html hippohtml="${document.answer}"/>
