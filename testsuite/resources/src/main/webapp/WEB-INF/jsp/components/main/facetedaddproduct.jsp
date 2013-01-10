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
    <h1>Add Sample Cars</h1>
    <p>
      Want to see faceted navigation in action? Click the buttons to add some dummy cars.<br />
      <b>Note:</b> adding more than approximately 1000 cars takes some time.
    </p>
    <form action="<hst:actionURL/>" method="get">
       <input type="hidden" name="number" value="5"/>
       <input type="submit" value="Add 5 more random cars"/>
    </form>
    <form action="<hst:actionURL/>" method="get">
        <input type="hidden" name="number" value="25"/>
        <input type="submit" value="Add 25 more random cars"/>
    </form>
    <form action="<hst:actionURL/>" method="get">
        <input type="hidden" name="number" value="100"/>
        <input type="submit" value="Add 100 more random cars"/>
    </form>
    <form action="<hst:actionURL/>" method="get">
        <input type="hidden" name="number" value="250"/>
        <input type="submit" value="Add 250 more random cars"/>
    </form>
</div>
