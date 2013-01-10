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
<%@ page contentType="text/html; charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<div id="bd">
  <div id="yui-main">
    <div class="yui-b">
      <div class="yui-gf">
        <hst:include ref="leftmenu"/>
        <div class="yui-u">
          <br/>
          HORIZONTAL SPAN
          <hst:include ref="span-content" /><br/>
          VERTICAL DIV
          <hst:include ref="div-content" />
          <br/>
          TABLE 
          <hst:include ref="table-content" />
          <br/>
          UL
          <hst:include ref="ul-content" />
          <br/>
          OL
          <hst:include ref="ol-content" />
        </div>
      </div>
    </div>
  </div>
  <div class="yui-b">
    <hst:include ref="right" />
  </div>
</div>