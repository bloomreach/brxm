<%--
  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)

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
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<hst:element name="link" var="jquerytheme">
  <hst:attribute name="id" value="jquery-theme"/>
  <hst:attribute name="media" value="all"/>
  <hst:attribute name="rel" value="stylesheet" />
  <hst:attribute name="type" value="text/css"/>
  <hst:attribute name="href" value="http://code.jquery.com/ui/1.8.18/themes/base/jquery-ui.css"/>
</hst:element>
<hst:headContribution keyHint="jquery-theme" element="${jquerytheme}"/>

<hst:element name="script" var="jquery">
  <hst:attribute name="type" value="text/javascript"/>
  <hst:attribute name="src" value="https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"/>
</hst:element>
<hst:headContribution keyHint="jquery" element="${jquery}" category="jsExternal"/>

<hst:element name="script" var="jqueryUI">
  <hst:attribute name="type" value="text/javascript"/>
  <hst:attribute name="src" value="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.18/jquery-ui.min.js"/>
</hst:element>
<hst:headContribution keyHint="jqueryUI" element="${jqueryUI}" category="jsExternal"/>

<hst:headContribution category="jsInline">
  <script type="text/javascript">
  $(function() {
    $( "#fromdate" ).datepicker();
    $( "#todate" ).datepicker();
    $( "#fromdateDay" ).datepicker();
    $( "#todateDay" ).datepicker();
  });
  </script>
</hst:headContribution>



<div id="yui-u">
  <hst:link var="searchURL" path="/search" />
  <hst:link var="cal_icon" path="/images/icons/calendar_icon.gif"/>
     <div>
       <b>SEARCH WITH FAST HIPPO REPO DATE RANGE QUERIES</b>
       <form action="${searchURL}" method="get">
         <table>
           <tr><td>Query:</td><td></td><td><input type="text" name="query" value="${query}"/></td>
           </tr>
           <tr><td>Page size:</td><td></td><td><input type="text" name="pageSize" value="${pageSize}" size="5"/></td>
           </tr>
         <tr>
           <td><b>From date:</b> <td><label for="fromdateDay"><img src="${cal_icon}"/></label></td>
           </td>
           <td>
             <input type="text" name="fromdateDay" id="fromdateDay" value="${fromdateDay}"/>
           </td>
         </tr>
         <tr>
           <td>
             <b>To date:</b>  <td><label for="todateDay"><img src="${cal_icon}"/></label></td>
           </td>
           <td>
             <input type="text" name="todateDay" id="todateDay" value="${todateDay}"/>
           </td>
         </tr>
       </table>
       <br/>
       <input type="submit" value="Search FAST RANGES"/>
       </form>
     </div>
    <br/><br/>
  <div>
    <b>SEARCH WITH SLOW JACKRABBIT DATE RANGE QUERIES</b>
    <form action="${searchURL}" method="get">
      <table>
        <tr><td>Query:</td><td></td><td><input type="text" name="query" value="${query}"/></td>
        </tr>
        <tr><td>Page size:</td><td></td><td><input type="text" name="pageSize" value="${pageSize}" size="5"/></td>
      </tr>
        <tr>
          <td><b>From date:</b> <td><label for="fromdate"><img src="${cal_icon}"/></label></td>
          </td>
          <td>
            <input type="text" name="fromdate" id="fromdate" value="${fromdate}"/>
          </td>
        </tr>
        <tr>
          <td>
            <b>To date:</b> <td><label for="todate"><img src="${cal_icon}"/></label></td>
          </td>
          <td>
            <input type="text" name="todate" id="todate" value="${todate}"/>
          </td>
        </tr>
      </table>
      <br/>
      <input type="submit" value="Search"/>
    </form>
  </div>

  <p>
     <br/>
     <hst:componentRenderingURL var="componentRenderingURL">
        <hst:param name="page" value="${crPage}" />
        <hst:param name="query" value="${query}" />
     </hst:componentRenderingURL>
     <a href="${componentRenderingURL}">Render only this component</a>
     
   </p>

   <c:if test="${query != null && query != ''}">

    <c:choose>
      <c:when test="${result.totalSize > 0}">
        <p></p>
        <p><b>${result.totalSize}</b> results for <b>${query}</b>.</p>

        <c:forEach var="bean" items="${result.hippoBeans}"
          varStatus="indexer">
          <hst:link var="link" hippobean="${bean}" />
          <ul class="list-overview">
            <li class="title">
              <c:if test="${hst:isNodeType(bean.node, 'demosite:newsdocument')}">
                <img src="<hst:link path='/images/news.png'/>"/>
              </c:if>
              <a href="${link}">${bean.title}</a>
              <div>
                <c:if test="${hst:isReadable(bean, 'date')}">
                  <p><fmt:formatDate value="${bean.date.time}" type="Date" pattern="MMMM d, yyyy h:mm a" /></p>
                </c:if>
                <c:if test="${hst:isReadable(bean, 'summary')}">
                  <p>${bean.summary}</p>
                </c:if>
              </div>
            </li>
          </ul>
        </c:forEach>

        <c:if test="${fn:length(pages) gt 0}">
          <ul id="paging-nav">
            <c:forEach var="page" items="${pages}">
              <c:set var="active" value="" />
              <c:choose>
                <c:when test="${crPage == page}">
                  <li>${page}</li>
                </c:when>
                <c:otherwise>
                  <hst:renderURL var="pagelink">
                    <hst:param name="page" value="${page}" />
                  </hst:renderURL>
                  <li><a href="${pagelink}" title="${page}">${page}</a></li>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </ul>
        </c:if>

      </c:when>
      <c:otherwise>
        <p></p>
        <p>No results for <b>${query}</b>.</p>
      </c:otherwise>
    </c:choose>

  </c:if>
</div>
