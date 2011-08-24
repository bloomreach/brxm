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
<%@ page contentType="text/html; charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<hst:headContribution keyHint="title"><title>${document.title}</title></hst:headContribution>

<div class="yui-u">
  <h1>${document.title}</h1>
  <p>${document.summary}</p>
  <hst:html hippohtml="${document.html}"/>

  <p> </p>
  
  <hst:link var="resource" hippobean="${document.resource}" />
  <a href="${resource}">${document.resource.name}</a>
  
  <div>
    <hst:include ref="todolist"/>
  </div>

   <hr/>
  <p>Dummy Example REST links </p>
  
  <hst:link var="imageset" hippobean="${image}"  mount="restapi-gallery"/>
  <hst:link var="thumbnail" hippobean="${image}" mount="restapi-gallery" subPath="thumbnail" />
  <hst:link var="original" hippobean="${image}"  mount="restapi-gallery"  subPath="original"/>
  
  <p> ImageSet : <a target="_blank" href="${imageset}">${imageset}</a></p>
  <p> Thumbnail : <a target="_blank" href="${thumbnail}">${thumbnail}</a></p>
  <p> Original : <a target="_blank" href="${original}">${original}</a></p>
  <p style="height: 30em;">  </p>
  
  <p style="height: 30em;">  </p>
  
  
</div>

