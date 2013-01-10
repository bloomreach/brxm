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
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<hst:headContribution keyHint="title"><title>Faceted Navigation Add Products</title></hst:headContribution>

<div id="yui-u">
    <c:if test="${not empty message}">
        <h2>${message}</h2>
    </c:if>

    <h1>Add Wikipedia Documents</h1>
    <p>
      Below type in the number of wikipedia documents you want to import. If you want to import more than 100 documents or if you want to add documents of your own
      downloaded wiki, you need to specify the filesystem location where the wiki xml file can be found, for example /home/usr/Downloads/enwiki-20100622-pages-articles.xml <br />
      <b>Note:</b> adding more than approximately 1000 wikipedia item will take some time.
    </p>
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
    
    <form action="<hst:actionURL/>" method="get">
        <table>
          <tr>
            <td>Number: </td>
            <td><input type="text" name="number"/></td>
          </tr>
          <tr>
            <td>Offset: </td>
            <td><input type="text" name="offset" value="0"/></td>
          </tr>
          <tr>
            <td>Maximum number of documents per folder: </td>
            <td><input type="text" name="maxDocsPerFolder" value=""/></td>
          </tr>
          <tr>
            <td>Maximum number of subfolders: </td>
            <td><input type="text" name="maxSubFolder" value=""/></td>
          </tr>
          <tr>
            <td>Wiki location on filesystem: </td>
            <td><input type="text" name="filesystemLocation"/></td>
          </tr>
          <tr>
            <td>Number of relations: </td>
	        <td><input type="text" name="relate"/></td>
          </tr>
          <tr>
            <td>Number of links: </td>
            <td><input type="text" name="link"/></td>
          </tr>
          <tr>
            <td>Number of translations: </td>
            <td><input type="text" name="translate"/></td>
          </tr>
          <tr>
            <td>Include images: </td>
            <td><input type="checkbox" name="images" /></td>
          </tr>
          <tr>
            <td></td>
            <td><input type="submit" value="Add wikipedia docs"/></td>
          </tr>
        </table>
    </form>
</div>
