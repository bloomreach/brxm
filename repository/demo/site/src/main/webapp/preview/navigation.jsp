<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst" prefix="h" %>
<%--
    Copyright 2007 Hippo
    
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
<div id="bar" width="100%"><img src="hippoecm.gif"></div><br/>
<div id="menu">
  <c:if test="${webpage._name == 'index'}"><a href="index.jsp"><b>Home</b></a><br/></c:if>
  <c:if test="${webpage._name != 'index'}"><a href="index.jsp">Home</a><br/></c:if>

  <c:if test="${webpage._name == 'news'}"><a href="news.jsp">
    <b>News</b></a><br/>
    <c:forEach var="iterator" items="${context['/site/messages']}">
      <c:set var="message" value="${iterator[iterator._name]}"/>
      &nbsp;&nbsp;<small><a href="message.jsp?message=${message._name}">${message['demo:title']}</a></small><br/>
    </c:forEach>
  </c:if>
  <c:if test="${webpage._name != 'news'}"><a href="news.jsp">News</a><br/></c:if>

  <c:if test="${webpage._name == 'contact'}"><a href="contact.jsp"><b>Contact</b></a><br/></c:if>
  <c:if test="${webpage._name != 'contact'}"><a href="contact.jsp">Contact</a><br/></c:if>
</div>
