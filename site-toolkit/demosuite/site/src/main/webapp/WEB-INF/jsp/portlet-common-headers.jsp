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
<%@ page language="java" session="false" import="javax.portlet.*, org.w3c.dom.*" %>
<%
RenderResponse renderResponse = (RenderResponse) request.getAttribute("javax.portlet.response");
Element styleElem = renderResponse.createElement("LINK");
styleElem.setAttribute("id", "demo-site-styles");
styleElem.setAttribute("rel", "stylesheet");
styleElem.setAttribute("href", request.getContextPath() + "/css/style.css");
styleElem.setAttribute("type", "text/css");
renderResponse.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, styleElem);
%>
