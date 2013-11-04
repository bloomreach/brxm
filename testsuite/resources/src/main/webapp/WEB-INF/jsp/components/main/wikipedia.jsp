<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

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
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst' %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<hst:headContribution keyHint="title"><title>Add Wikipedia Products</title></hst:headContribution>

${paramValues.maxDocsPerFolder}
<hst:defineObjects />

<%--Setting paramater default values and filling out values as submitted before. Yes this could better have been done in java code --%>
<c:set var="paramNumber"><%= hstRequest.getRequestContext().getServletRequest().getParameter("number")%></c:set>
<c:set var="defaultNumber" value="5" ></c:set>
<c:choose>
  <c:when test="${paramNumber eq 'null'}"><c:set var="number" value="${defaultNumber}" ></c:set></c:when>
  <c:otherwise><c:set var="number" value="${paramNumber}" ></c:set>
  </c:otherwise>
</c:choose>

<c:set var="paramOffset"><%= hstRequest.getRequestContext().getServletRequest().getParameter("offset")%></c:set>
<c:set var="defaultOffset" value="0" ></c:set>
<c:choose>
  <c:when test="${paramOffset eq 'null'}"><c:set var="offset" value="${defaultOffset}" ></c:set></c:when>
  <c:otherwise><c:set var="offset" value="${paramOffset}" ></c:set>
  </c:otherwise>
</c:choose>

<c:set var="paramMaxDocsPerFolder"><%= hstRequest.getRequestContext().getServletRequest().getParameter("maxDocsPerFolder")%></c:set>
<c:set var="defaultMaxDocsPerFolder" value="200" ></c:set>
<c:choose>
  <c:when test="${paramMaxDocsPerFolder eq 'null'}"><c:set var="maxDocsPerFolder" value="${defaultMaxDocsPerFolder}" ></c:set></c:when>
  <c:otherwise><c:set var="maxDocsPerFolder" value="${paramMaxDocsPerFolder}" ></c:set>
  </c:otherwise>
</c:choose>

<c:set var="paramMaxSubFolder"><%= hstRequest.getRequestContext().getServletRequest().getParameter("maxSubFolder")%></c:set>
<c:set var="defaultMaxSubFolder" value="50" ></c:set>
<c:choose>
  <c:when test="${paramMaxSubFolder eq 'null'}"><c:set var="maxSubFolder" value="${defaultMaxSubFolder}" ></c:set></c:when>
  <c:otherwise><c:set var="maxSubFolder" value="${paramMaxSubFolder}" ></c:set>
  </c:otherwise>
</c:choose>

<c:set var="paramFilesystemLocation"><%= hstRequest.getRequestContext().getServletRequest().getParameter("filesystemLocation")%></c:set>
<c:set var="defaultFilesystemLocation" value="" ></c:set>
<c:choose>
  <c:when test="${paramFilesystemLocation eq 'null'}"><c:set var="filesystemLocation" value="${defaultFilesystemLocation}" ></c:set></c:when>
  <c:otherwise><c:set var="filesystemLocation" value="${paramFilesystemLocation}" ></c:set>
  </c:otherwise>
</c:choose>

<c:set var="paramRelate"><%= hstRequest.getRequestContext().getServletRequest().getParameter("relate")%></c:set>
<c:set var="defaultRelate" value="0" ></c:set>
<c:choose>
  <c:when test="${paramRelate eq 'null'}"><c:set var="relate" value="${defaultRelate}" ></c:set></c:when>
  <c:otherwise><c:set var="relate" value="${paramRelate}" ></c:set>
  </c:otherwise>
</c:choose>

<c:set var="paramLink"><%= hstRequest.getRequestContext().getServletRequest().getParameter("link")%></c:set>
<c:set var="defaultLink" value="0" ></c:set>
<c:choose>
  <c:when test="${paramLink eq 'null'}"><c:set var="link" value="${defaultLink}" ></c:set></c:when>
  <c:otherwise><c:set var="link" value="${paramLink}" ></c:set>
  </c:otherwise>
</c:choose>

<c:set var="paramTranslate"><%= hstRequest.getRequestContext().getServletRequest().getParameter("translate")%></c:set>
<c:set var="defaultTranslate" value="0" ></c:set>
<c:choose>
  <c:when test="${paramLink eq 'null'}"><c:set var="translate" value="${defaultTranslate}" ></c:set></c:when>
  <c:otherwise><c:set var="translate" value="${paramTranslate}" ></c:set>
  </c:otherwise>
</c:choose>


<div id="yui-u">
  <c:if test="${not empty message}">
    <p style="color:red;">${message}</p>
  </c:if>

  <h1>Add Wikipedia Documents </h1>
  <hr/>

  <h2>Quick add wikipedia documents</h2>

  <form action="<hst:actionURL/>" method="get">
    <input type="hidden" name="number" value="5"/>
    <input type="submit" value="Add 5 wikipedia docs"/>
  </form>
  <form action="<hst:actionURL/>" method="get">
    <input type="hidden" name="number" value="10"/>
    <input type="submit" value="Add 10 wikipedia docs"/>
  </form>
  <form action="<hst:actionURL/>" method="get">
    <input type="hidden" name="number" value="100"/>
    <input type="submit" value="Add 100 wikipedia docs"/>
  </form>
  <hr/>
  <br/>

  <h2>Custom add wikipedia documents</h2>
  <form action="<hst:actionURL/>" method="get">
    <table>
      <tr>
        <td><b>Number of documents to add:</b>
          <br />
          <p style="font-style: italic;">
          More than 100 documents? If you want to import more than 100 documents you need to specify the wiki download local file below<br/>
          More than 1000 documents? this will take some time.
         </p>


        </td>
        <td><input type="text" name="number" value="${number}"/></td>
      </tr>
      <tr>
        <td><b>Start index in xml file (offset):</b> <br />
          <p style="font-style: italic;">
            Example: Suppose you already imported 50 documents and want to import another 10. If you don't want to import the same documents, you can fill in 50 here.
          </p>

        </td>
        <td><input type="text" name="offset" value="${offset + number}"/></td>
      </tr>
      <tr>
        <td><b>Maximum number of documents per folder:</b></td>
        <td><input type="text" name="maxDocsPerFolder" value="${maxDocsPerFolder}"/></td>
      </tr>
      <tr>
        <td><b>Maximum number of subfolders:</b></td>

        <td><input type="text" name="maxSubFolder" value="${maxSubFolder}"/></td>
      </tr>
      <tr>
        <td><b>Wiki location on filesystem:   </b>  <br />

          <p style="font-style: italic;">Example /home/usr/Downloads/enwiki-20100622-pages-articles.xml <br /> Use this for more than 100 documents or a custom set
          <br />
          You can download these article files here: <a href="http://dumps.wikimedia.org/enwiki/latest/">http://dumps.wikimedia.org/enwiki/latest/</a>
          </p>


        </td>
        <td><input type="text" name="filesystemLocation" value="${filesystemLocation}"/></td>
      </tr>
      <tr>
        <td><b>Number of relations:</b><br />
          <p style="font-style:italic;">Generated amount of links in related docs.</p>

        </td>

        <td><input type="text" name="relate" value="${relate}" /></td>
      </tr>
      <tr>
        <td><b>Number of links:</b>
        <p style="font-style:italic;">These are automated links added in the block contents, after the body tag</p>       </td>
        <td><input type="text" name="link" value="${link}"/></td>
      </tr>
      <tr>
        <td><b>Number of translations:</b><br />
          <p style="font-style:italic;">0 means only English documents are generated. </p>
        </td>
        <td><input type="text" name="translate" value="${translate}"/></td>
      </tr>
      <tr>
        <td><b>Include (fake) images:</b><br />
        </td>
        <td><input type="checkbox" name="images" value="<%= hstRequest.getRequestContext().getServletRequest().getParameter("images")%>"/></td>
      </tr>
      <tr>
        <td></td>
        <td><input type="submit" value="Add wikipedia docs"/></td>
      </tr>
    </table>
  </form>
</div>
