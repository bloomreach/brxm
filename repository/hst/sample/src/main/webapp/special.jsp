<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst" prefix="h" %>
<c:set var="global" value="${context['/hst-sample/global/global']}" scope="page"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"><head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <link href="/css/screen.css" rel="stylesheet" type="text/css" media="screen"/>
  <link href="/css/print.css" rel="stylesheet" type="text/css" media="print"/>
</head><body>
  <div id="canvas">

  <div id="block"><br/><br/></div>

    <h1>Viewing special node</h1>
    <blockquote>${context._path}<p/></blockquote>

    <table cellspacing="2"><tr>
      <td>title</td><td>:</td><td>${context.title}</td>
    </tr><tr>
      <td>state</td><td>:</td><td>${context.state}</td>
    </tr><tr>
      <td>link</td><td>:</td><td>${context.link}</td>
    </tr></table>

  </div>
</body></html>
