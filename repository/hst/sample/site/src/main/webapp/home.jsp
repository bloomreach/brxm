<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%--
    Copyright 2008 Hippo
    
    Licensed under the Apache License, Version 2.0 (the  "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS"
    BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--%>
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst" prefix="hst" %>
<html xmlns="http://www.w3.org/1999/xhtml"><head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <link href="/css/screen.css" rel="stylesheet" type="text/css" media="screen"/>
  <link href="/css/print.css" rel="stylesheet" type="text/css" media="print"/>
</head>
<body>
  <div id="canvas">

    <jsp:include page="navigation.jsp"/>

    <h3>${context._path}</h3>

    <div id="left">
	  <hst:context var="docs" location="../../../documents">
        <h4><hst:content context="docs" property="_path"/></h4>
	    <c:forEach var="handle" items="${docs}">
		  <hst:context var="item" context="handle" location="${handle._name}">
		  	<hst:content context="item" property="hstsample:title"/><br/>
		  </hst:context>
	    </c:forEach>
      </hst:context>
    </div>

    <div id="right">
    <h4>${defaultContext._path}/${defaultContext.articles._name}</h4>
	   	<hst:articles context="defaultContext"/>

    <h4>${defaultContext._path}/${defaultContext.news._name}</h4>
    	<hst:news context="defaultContext"/>

    <h4>${defaultContext._path}/${defaultContext.events._name}</h4>
	   	<hst:events context="defaultContext"/>
   </div>

  </div>
</body></html>
