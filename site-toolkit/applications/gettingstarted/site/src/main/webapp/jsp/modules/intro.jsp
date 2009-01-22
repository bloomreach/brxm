<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>

<hst-tmpl:module name="intro" var="item" className="org.hippoecm.hst.components.modules.content.ContentModule" render="true" execute="false"/>
<c:if test="${item != null}">
	<h1>${item.property['gettingstarted:title']}</h1>
	<p>${item.property['gettingstarted:summary']}</p>
	<p>${item.node['gettingstarted:body'].property['hippostd:content']}</p>
</c:if>