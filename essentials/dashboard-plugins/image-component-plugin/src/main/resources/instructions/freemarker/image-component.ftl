{{#repositoryBased}}
    <#include "../../hst:default/hst:templates/imports.ftl">
{{/repositoryBased}}
{{#fileBased}}
    <#include "/WEB-INF/freemarker/include/imports.ftl">
{{/fileBased}}
<#--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#-- @ftlvariable name="document" type="org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean" -->
<#if document??>
<@hst.link var="img" hippobean=document.original/>
    <img src="${img}" title="${document.fileName}" alt="${document.fileName}"/>
</#if>
<#if editMode && document>
    <@hst.link var="placholderLink" path="/images/essentials-edit-component.png" />
    <img src="${placholderLink}" alt="Edit image component settings">
</#if>

