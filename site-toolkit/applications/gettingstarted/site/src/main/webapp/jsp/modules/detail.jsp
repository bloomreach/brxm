<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>


<hst-tmpl:node var="contentNode"/>
<h1>${contentNode.property['gettingstarted:title'] }</h1>

<p>${contentNode.node['gettingstarted:body'].property['hippostd:content']}</p>

