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
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<table width="100%">
  <tr>
    <td align="left">
      <h1>Taxonomy Demo Website</h1>
    </td>
    <td align="right">
      <a href="<hst:link path='/' mount='site' />">USA</a>
      |
      <a href="<hst:link path='/'  mount='site-en-gb' />">UK</a>
      |
      <a href="<hst:link path='/'  mount='site-nl' />">NL</a>
    </td>
  </tr>
  <tr>
    <td align="right" colspan="2">
      <form action="<hst:link path="/search"/>">
        <input type="text" name="query"/>
        <input type="submit"/>
      </form>
    </td>
  </tr>
</table>