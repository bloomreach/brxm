<%@ page language="java" %>
<%@ taglib uri='/WEB-INF/hst-core.tld' prefix='hc'%>

<%
System.out.println("Console out from newspage.jsp");
%>

<html>
<head>
<title>News Page</title>
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
</head>
<body>

<div class="page">

<hc:content path="h" />

</div>

</body>
</html>