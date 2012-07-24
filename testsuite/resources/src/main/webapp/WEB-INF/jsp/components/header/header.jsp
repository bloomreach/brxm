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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<hst:link path="/css/yui-2.8.1-reset-fonts-grids.css" var="yuiResetFontsGridCssHref"/>
<hst:element var="yuiResetFontsGridCss" name="link">
  <hst:attribute name="id" value="yuiResetFontsGridCss" />
  <hst:attribute name="rel" value="stylesheet" />
  <hst:attribute name="type" value="text/css" />
  <hst:attribute name="href" value="${yuiResetFontsGridCssHref}" />
</hst:element>
<hst:link path="/css/style.css" var="demoSiteCssHref"/>
<hst:element var="demoSiteCss" name="link">
  <hst:attribute name="id" value="demoSiteCss" />
  <hst:attribute name="rel" value="stylesheet" />
  <hst:attribute name="type" value="text/css" />
  <hst:attribute name="href" value="${demoSiteCssHref}" />
</hst:element>
<hst:headContribution keyHint="yuiResetFontsGridCss" element="${yuiResetFontsGridCss}" />
<hst:headContribution keyHint="demoSiteCss" element="${demoSiteCss}" />

<hst:link var="destination"/>

  <div id="hd">        
    <div id="topnav" class="yui-gc">
      <div class="yui-u first"></div>
      <div class="yui-u">
        <ul class="menu">
          <li>
            <a title="Contact" href="<hst:link path="/content/contact"/>">Contact</a>
          </li>
          <% if (request.getUserPrincipal() == null) { %>
          <li>
            <hst:link var="login" path="/login/form" />
            <a title="Login" href="${login}?destination=${destination}">Log In</a>
          </li>
          <% } else { %>
          <li>
            <span><%=request.getUserPrincipal().getName()%></span>&nbsp;
            <hst:link var="logout" path="/login/logout" />
            <a title="Logout" href="${logout}?destination=${destination}">Log Out</a>
          </li>
          <% } %>
        </ul>
      </div>
    </div> 
    <div id="nav">
      <ul class="menu">
        <hst:link var="flag" path="/images/icons/flag-16_${pageContext.request.locale.language}.png"/>
        <li>
            <img src="${flag}"/> ${pageContext.request.locale.displayCountry}
        </li>
        
        <!-- BELOW WE SHOW EXAMPLES OF LINKS BY *REF-ID* AND SHOW I18N WITHOUT NEEDING TO SET THE LOCALE: THE LOCALE IS
             RETRIEVED FROM THE BACKING SITEMAPITEM/MOUNT 
        -->
        <hst:link var="home" siteMapItemRefId="homeId"/>
        <li>
            <a href="${home}"><fmt:message key="key.home"/> </a>
        </li>
        <hst:link var="about" siteMapItemRefId="aboutId"/>
        <li>
            <a href="${about}"><fmt:message key="key.aboutus"/></a>
        </li>
        
      </ul>
    </div>  
  </div>
  
