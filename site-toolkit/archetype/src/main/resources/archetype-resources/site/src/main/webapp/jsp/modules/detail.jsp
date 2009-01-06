#set($hyphen = '-')
#set($empty = '')
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>


<hst-tmpl:node var="contentNode"/>
<h1>${dollar}{contentNode.property['${rootArtifactId.replace($hyphen,$empty)}:title'] }</h1>

<p>${dollar}{contentNode.node['${rootArtifactId.replace($hyphen,$empty)}:body'].property['hippostd:content']}</p>

