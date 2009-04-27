#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
  <ul class="sitenav">
    <c:forEach var="item" items="${symbol_dollar}{menu.siteMenuItems}">
        <li>
            <c:choose >
                <c:when test="${symbol_dollar}{item.selected}">
                    <b>${symbol_dollar}{item.name}</b>
                </c:when>
                <c:otherwise>
                    <hst:link var="link" link="${symbol_dollar}{item.hstLink}"/>
                    <a href="${symbol_dollar}{link}">
                        ${symbol_dollar}{item.name}
                    </a>
                </c:otherwise>
            </c:choose>
            <c:if test="${symbol_dollar}{item.expanded}">
            <ul>
                <c:forEach var="subitem" items="${symbol_dollar}{item.childMenuItems}">
                    <c:choose >
                    <c:when test="${symbol_dollar}{subitem.selected}">
                        <li>
                        <b>${symbol_dollar}{subitem.name}</b>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <hst:link var="link" link="${symbol_dollar}{subitem.hstLink}"/>
                        <li>
                        <a href="${symbol_dollar}{link}">
                            ${symbol_dollar}{subitem.name}
                        </a>
                        </li>
                    </c:otherwise>
                    </c:choose>
                        <c:if test="${symbol_dollar}{subitem.expanded}">
                        <ul>
                        <c:forEach var="subsubitem" items="${symbol_dollar}{subitem.childMenuItems}">
                            <c:choose >
                            <c:when test="${symbol_dollar}{subsubitem.selected}">
                                <li>
                                <b>${symbol_dollar}{subsubitem.name}</b>
                                </li>
                            </c:when>
                            <c:otherwise>
                                <hst:link var="link" link="${symbol_dollar}{subsubitem.hstLink}"/>
    <li>
                                <a href="${symbol_dollar}{link}">
                                    ${symbol_dollar}{subsubitem.name}
                                </a>
                                </li>
                            </c:otherwise>
                            </c:choose>
                        </c:forEach>
                        </ul>
                        </c:if>
                </c:forEach>
            </ul>
            </c:if>
    </li>
    </c:forEach>
  </ul>
</div>
