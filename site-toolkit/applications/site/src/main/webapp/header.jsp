<%@ page language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri='/WEB-INF/hst-core.tld' prefix='hc'%>

<%
System.out.println("Console out from header.jsp");
%>

<div class="header">

<hc:content path="t" />

<script language="javascript">
<!--
function <hc:namespace/>showPopup() {
    alert("Hello from header component!");
}
//-->
</script>

<div>

    <a href="javascript:<hc:namespace/>showPopup();">Show</a>

    <hc:url var="firstUrl" type="render">
      <hc:param name="page" value="1" />
    </hc:url>
    <hc:url var="lastUrl" type="render">
      <hc:param name="page" value="9" />
    </hc:url>
    
    <a href="<%=firstUrl%>">First</a>
    <a href="<%=lastUrl%>">Last</a>
    
    <div>
        header parameters: <%=request.getParameterMap()%>
    </div>
    
</div>

</div>