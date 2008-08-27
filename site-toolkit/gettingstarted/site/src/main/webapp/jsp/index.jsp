<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
 <head>
  <jsp:include page="includes/head.jsp"></jsp:include>
 </head>
 <body>	
  <!-- wrapper -->
  <div id="wrapper">
    <div id="mainNav">
      <hst-tmpl:container name="nav"/>
    </div>
    <div id="content">
      <hst-tmpl:container name="content"/>
    </div>		
  </div>
 </body>
</html>
