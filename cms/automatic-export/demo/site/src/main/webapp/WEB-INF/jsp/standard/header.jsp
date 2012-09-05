<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="headerName" type="java.lang.String"--%>

<h1><c:out value="${headerName}"/></h1>

<fmt:message var="submitText" key="search.submit.text"/>
<hst:link var="link" path="/search"/>
<form action="${link}" method="POST">
 <input type="text" name="query"/>
 <input type="submit" value="${submitText}"/>
</form>