<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>

<fmt:setBundle basename="org.hippoecm.hst.security.servlet.LoginServlet" />

<%
String destination = (String) request.getAttribute("org.hippoecm.hst.security.servlet.destination");
if (destination == null) destination = "";

int autoRedirectSeconds = 2;

%>

<hst:link var="loginFormUrl" path="/login/form" >
  <hst:param name="destination" value="<%=destination%>" />
</hst:link>
<hst:link var="loginProxyUrl" path="/login/proxy" >
  <hst:param name="destination" value="<%=destination%>" />
</hst:link>

<html>
  <head>
    <title>
       <fmt:message key="label.authen.required" />
    </title>
    <meta http-equiv='refresh' content='<%=autoRedirectSeconds%>;url=${loginFormUrl}' />
    <link rel="stylesheet" type="text/css" href="<hst:link path='/login/hst/security/skin/screen.css'/>" />
  </head>
  <body class="hippo-root">
    <div>
      <div class="hippo-login-panel">
        <form class="hippo-login-panel-form" name="signInForm" method="post" action="${loginProxyUrl}">
          <h2><div class="hippo-global-hideme"><span>Hippo CMS 7</span></div></h2>
          <div class="hippo-login-form-container">
            <table>
              <tr>
                <td>
                  <p>
                    <fmt:message key="message.authen.required">
                      <fmt:param value="<%=destination%>" />
                    </fmt:message>
                </td>
              </tr>
              <tr>
                <td>
                  <p>
                    <a href="${loginFormUrl}"><fmt:message key="message.try.again"/></a>
                    <br/><br/>
                    <fmt:message key="message.page.auto.redirect.in.seconds">
                      <fmt:param value="<%=autoRedirectSeconds%>" />
                    </fmt:message>
                  </p>
                </td>
              </tr>
            </table>
          </div>
        </form>
        <div class="hippo-login-panel-copyright">
          &copy; 1999-2011 Hippo B.V.
        </div>
      </div>
    </div>
  </body>
</html>