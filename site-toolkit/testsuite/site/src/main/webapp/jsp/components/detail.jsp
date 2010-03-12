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
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<div>

  <h2>My detail page</h2>
  
  <div style="border:1px black solid; width:400px;">
    <hst:link var="link" hippobean="${parent}"/>
    <a href="${link}">PARENT : ${parent.name}</a>
  </div>
    
  <div style="border:1px black solid; width:400px;">
    ${document.title}
    <br/>
    ${document.summary}
    <br/>
    ${document.date.time}
    <div style="border:1px black solid;" >
      <hst:html hippohtml="${document.body}"/>
    </div>
  </div>
  
  <div>
    page#: ${param.page}
    [ 
       <a href="<hst:renderURL><hst:param name="page" value="1"/></hst:renderURL>">1</a> |
       <a href="<hst:renderURL><hst:param name="page" value="2"/></hst:renderURL>">2</a> |
       <a href="<hst:renderURL><hst:param name="page" value="3"/></hst:renderURL>">3</a> |
       <a href="<hst:renderURL><hst:param name="page"/></hst:renderURL>">reset</a>
    ]
  </div>
  
  <br/>
  
  <div>
    <h3>Comments</h3>
    <ul>
      <c:forEach var="comment" items="${comments}">
        <hst:actionURL var="removeURL">
          <hst:param name="type" value="remove"/>
          <hst:param name="path" value="${comment.path}"/>
        </hst:actionURL>
        <li>${comment.title} (<a title="Delete" href="${removeURL}">X</a>)
          <div style="background-color: #ffc">${comment.bodyContent}</div>
        </li>
      </c:forEach>
    </ul>
  </div>
  
  <hst:actionURL var="addURL">
    <hst:param name="type" value="add"/>
  </hst:actionURL>
  
  <div>
    <form method="POST" action="${addURL}">
      <h4>Enter your comment here:</h4>
      <table>
        <tr>
          <th>Title:</th>
          <td><input type="text" name="title" value="" /></td>
        </tr>
        <tr>
          <th valign="top">Comment:</th>
          <td><textarea name="comment" rows="4" cols="40"></textarea></td>
        </tr>
        <tr>
          <td colspan="2">
            <input type="submit" value="Submit"/>
		    <input type="reset" value="Reset"/>
          </td>
         </tr>
      </table>
    </form>
  </div>
  
</div>
