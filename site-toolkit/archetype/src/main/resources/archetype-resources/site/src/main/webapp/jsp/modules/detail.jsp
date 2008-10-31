#set($hyphen = '-')
#set($underscore = '_')
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>


<hst-tmpl:node var="contentNode"/>
<h1>${dollar}{contentNode.property['${rootArtifactId.replace($hyphen,$underscore)}:title'] }</h1>

<p>${dollar}{contentNode.property['${rootArtifactId.replace($hyphen,$underscore)}:body'] }</p>

