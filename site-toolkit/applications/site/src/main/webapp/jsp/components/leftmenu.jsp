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

<div style="border:1px solid">
    <h1>My menu : ${menu.name}</h1>
    <ul>
    <c:forEach var="item" items="${menu.menuItems}">
        <li>
            <c:choose >
                <c:when test="${item.selected}">
                    <b>${item.name}</b>
                </c:when>
                <c:otherwise>
                    <hst:link var="link" link="${item.hstLink}"/>
                    <a href="${link}">
                        ${item.name}
                    </a>
                </c:otherwise>
            </c:choose>
            <c:if test="${item.expanded}">
            <ul>
                <c:forEach var="subitem" items="${item.childMenuItems}">
                    <c:choose >
                    <c:when test="${subitem.selected}">
                        <li>
                        <b>${subitem.name}</b>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <hst:link var="link" link="${subitem.hstLink}"/>
                        <li>
                        <a href="${link}">
                            ${subitem.name}
                        </a>
                        </li>
                    </c:otherwise>
                    </c:choose>
                        <c:if test="${subitem.expanded}">
                            <ul>
                            <c:forEach var="subsubitem" items="${subitem.childMenuItems}">
                                <c:choose >
                                <c:when test="${subsubitem.selected}">
                                    <li>
                                    <b>${subsubitem.name}</b>
                                    </li>
                                </c:when>
                                <c:otherwise>
                                    <hst:link var="link" link="${subsubitem.hstLink}"/>
                                    <li>
                                    <a href="${link}">
                                        ${subsubitem.name}
                                    </a>
                                    </li>
                                </c:otherwise>
                                </c:choose>
                                <c:if test="${subitem.expanded}">
                                <ul>
                                    <c:forEach var="subsubsubitem" items="${subsubitem.childMenuItems}">
                                        <c:choose >
                                        <c:when test="${subsubsubitem.selected}">
                                            <li>
                                            <b>${subsubsubitem.name}</b>
                                            </li>
                                        </c:when>
                                        <c:otherwise>
                                            <hst:link var="link" link="${subsubsubitem.hstLink}"/>
                                            <li>
                                            <a href="${link}">
                                                ${subsubsubitem.name}
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
                </c:forEach>
            </ul>
            </c:if>
        </li>
    </c:forEach>
    </ul>
</div>