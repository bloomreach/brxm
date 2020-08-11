<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>

<fmt:setBundle basename="org.hippoecm.hst.security.servlet.LoginServlet"/>

<%
  String destination = "";
  HttpSession session = pageContext.getSession();
  if (session != null) {
    destination = (String) session.getAttribute("org.hippoecm.hst.security.servlet.destination");
    if (destination == null) destination = "";
  }
  int autoRedirectSeconds = 2;
%>

<hst:link var="loginFormUrl" path="/login/form">
  <hst:param name="destination" value="<%=destination%>"/>
</hst:link>
<hst:link var="loginProxyUrl" path="/login/proxy">
  <hst:param name="destination" value="<%=destination%>"/>
</hst:link>

<html lang="en">
<head>
  <meta charset="utf-8"/>
  <title><fmt:message key="label.authen.required"/></title>
  <meta http-equiv="refresh" content='<%=autoRedirectSeconds%>;url=${loginFormUrl}'/>
  <link rel="stylesheet" type="text/css" href="<hst:link path='/login/hst/security/skin/screen.css'/>"/>
</head>
<body class="hippo-root">
<div>
  <div class="hippo-login-panel">
    <form class="hippo-login-panel-form" name="signInForm" method="post" action="${loginProxyUrl}">
      <h2>
        <div class="hippo-global-hideme"><span><fmt:message key="label.authen.required"/></span></div>
      </h2>
      <div class="hippo-login-form-container">
        <p>
          <fmt:message key="message.authen.required">
            <fmt:param value="<%=destination%>"/>
          </fmt:message>
        </p>
        <p>
          <a href="${loginFormUrl}"><fmt:message key="message.try.again"/></a>
          <br/><br/>
          <fmt:message key="message.page.auto.redirect.in.seconds">
            <fmt:param value="<%=autoRedirectSeconds%>"/>
          </fmt:message>
        </p>
      </div>
    </form>
  </div>
</div>
</body>
</html>