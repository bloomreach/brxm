<%--
  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<hst:defineObjects/>

<hst:link var="currentUrl" path="${baseUrl}"/>

<h1>Taxonomy tree</h1>

<div>
  <ul>
    <c:forEach var="category" items="${taxonomy.categories}">
      <li><a href="${currentUrl}/${category.path}">${category.getInfo(hstRequest.locale).getName()}</a>
        <ul>
          <c:forEach var="subcategory" items="${category.children}">
            <li>
              <a href="${currentUrl}/${subcategory.path}">${subcategory.getInfo(hstRequest.locale).getName()}</a>
              <ul>
                <c:forEach var="subsubcategory"
                  items="${subcategory.children}">
                  <li><a
                    href="${currentUrl}/${subsubcategory.path}">${subsubcategory.getInfo(hstRequest.locale).getName()}</a>
                    <ul>
                      <c:forEach var="subsubsubcategory"
                        items="${subsubcategory.children}">
                        <li><a
                          href="${currentUrl}/${subsubsubcategory.path}">${subsubsubcategory.getInfo(hstRequest.locale).getName()}</a>
                        </li>
                      </c:forEach>
                    </ul></li>
                </c:forEach>
              </ul>
            </li>
          </c:forEach>
        </ul>
      </li>
    </c:forEach>
  </ul>
</div>
