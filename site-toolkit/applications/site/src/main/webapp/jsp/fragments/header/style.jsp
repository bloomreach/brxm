<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri='/WEB-INF/hst-core.tld' prefix='hc'%>


<%
System.out.println("Console out from title.jsp");
%>

<c:forEach var="name" items="${test}">

${name}

</c:forEach>

<style>

.page {
    BACKGROUND-COLOR: #DDDDDD; 
}
.header {
    BACKGROUND-COLOR: #AAAAAA; 
}
.title {
    BACKGROUND-COLOR: #777777;
}
</style>