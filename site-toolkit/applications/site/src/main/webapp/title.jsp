<%@ page language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri='/WEB-INF/hst-core.tld' prefix='hc'%>

<%
System.out.println("Console out from title.jsp");
%>

<div class="title">

<h1>News Title</h1>


<script language="javascript">
<!--
function <hc:namespace/>showPopup() {
    alert("Hello from title component!");
}
//-->
</script>

<div>

    <a href="javascript:<hc:namespace/>showPopup();">Show</a>
    
    <hc:url var="prevUrl" type="render">
      <hc:param name="page" value="prev" />
    </hc:url>
    <hc:url var="nextUrl" type="render">
      <hc:param name="page" value="next" />
    </hc:url>
    
    <a href="<%=prevUrl%>">Previous</a>
    <a href="<%=nextUrl%>">Next</a>
    
</div>

</div>