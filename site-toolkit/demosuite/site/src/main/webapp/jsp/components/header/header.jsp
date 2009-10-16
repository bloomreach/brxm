<%--
  Copyright 2008-2009 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<hst:element var="yuiResetFontsGridCss" name="link">
  <hst:attribute name="id" value="yuiResetFontsGridCss" />
  <hst:attribute name="rel" value="stylesheet" />
  <hst:attribute name="type" value="text/css" />
  <hst:attribute name="href" value="http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css" />
</hst:element>
<hst:link path="/css/style.css" var="demoSiteCssHref"/>
<hst:element var="demoSiteCss" name="link">
  <hst:attribute name="id" value="demoSiteCss" />
  <hst:attribute name="rel" value="stylesheet" />
  <hst:attribute name="type" value="text/css" />
  <hst:attribute name="href" value="${demoSiteCssHref}" />
</hst:element>
<hst:head-contribution keyHint="yuiResetFontsGridCss" element="${yuiResetFontsGridCss}" />
<hst:head-contribution keyHint="demoSiteCss" element="${demoSiteCss}" />

  <div id="hd">        
    <div id="topnav" class="yui-gc">
      <div class="yui-u first"></div>
      <div class="yui-u">
        <ul class="menu">        
          <li><a title="contact" href="<hst:link path="/content/contact"/>">Contact</a></li>
        </ul>
      </div>
    </div> 
    <div id="nav">
      <ul class="menu">
        <li class="active">Tutorial</li>      
        <li><a href="http://www.onehippo.org/" title="News">Main documentation</a></li>
      </ul>
    </div>  
  </div>
  
