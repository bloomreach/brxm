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
<#-- @ftlvariable name="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu" -->
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#if menu??>
  <#if menu.siteMenuItems??>
    <ul class="nav nav-pills">
      <#list menu.siteMenuItems as item>
        <#if  item.selected || item.expanded>
          <li class="active"><a href="<@hst.link link=item.hstLink/>">${item.name}</a></li>
        <#else>
          <li><a href="<@hst.link link=item.hstLink/>">${item.name}</a></li>
        </#if>
      </#list>
    </ul>
  </#if>
  <@hst.cmseditmenu menu=menu/>
<#else>
  <#if editMode>
    <img src="<hst:link path='/images/essentials-edit-component.png' />"
         alt="Configure valid menu in component parameters"
         title="Configure valid menu in component parameters">
  </#if>
</#if>