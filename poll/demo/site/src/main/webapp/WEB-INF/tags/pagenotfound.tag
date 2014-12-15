<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<hst:element var="headTitle" name="title">
   <fmt:message key="page.not.found"/>
 </hst:element>
<hst:headContribution keyHint="headTitle" element="${headTitle}"/>
<h2><fmt:message key="page.not.found"/></h2>