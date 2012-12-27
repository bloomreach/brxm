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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>


<div class="yui-b">
  <p><fmt:message key="key.available.languages"/>:</p>
  <br/>
  <c:forEach var="language" items="${crBean.availableTranslations.translations}">
      <hst:link var="flag" path="/images/icons/flag-16_${language.localeString}.png"/>
      <hst:link var="link" hippobean="${language}"/>
      <!-- the equal comparator can be used to check whether the translation is the current bean  -->
      <c:choose>
         <c:when test="${language.equalComparator[crBean]}">
           <div>
             <img src="${flag}"/> ${language.localeString}  &nbsp;&nbsp;&nbsp;(current)
          </div>
         </c:when>
         <c:otherwise>
            <div>
              <a href="${link}" style="text-decoration:underline">
               <img src="${flag}"/> ${language.localeString}
              </a>
            </div>
         </c:otherwise>
      </c:choose>
  </c:forEach>

</div>
