<%@ page contentType="text/html; charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>


<div role="main" class="yui-u">
  <h1>${document.title}</h1>
  <p>${document.summary}</p>
  <p><hst:html hippohtml="${document.html}"/></p>
  <p style="height: 30em;">  </p>
</div>

