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
  limitations under the License.
--%>
#set($hyphen = '-')
#set($empty = '')
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>


<hst-tmpl:node var="contentNode"/>
<h1>${dollar}{contentNode.property['${rootArtifactId.replace($hyphen,$empty)}:title'] }</h1>

<p>${dollar}{contentNode.node['${rootArtifactId.replace($hyphen,$empty)}:body'].property['hippostd:content']}</p>

