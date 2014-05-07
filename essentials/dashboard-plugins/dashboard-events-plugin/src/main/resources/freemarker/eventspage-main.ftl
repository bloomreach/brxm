<#include "/WEB-INF/freemarker/include/imports.ftl">
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
<#-- @ftlvariable name="document" type="{{beansPackage}}EventsDocument" -->
<#if document??>
    <@hst.link var="link" hippobean=document/>
<article>
    <@hst.cmseditlink hippobean=document/>
    <h3><a href="${link}">${document.title}</a></h3>
    <#if document.date??>
        <p><@fmt.formatDate value=document.date.time type="both" dateStyle="medium" timeStyle="short"/></p>
    </#if>
    <#if document.endDate??>
        <p><@fmt.formatDate value=document.endDate.time type="both" dateStyle="medium" timeStyle="short"/></p>
    </#if>
    <#if document.location??>
        <p>${document.location}</p>
    </#if>
    <#if document.introduction??>
        <p>${document.introduction}</p>
    </#if>
    <#if image.original>
        <@hst.link var="img" hippobean=document.image.original/>
        <figure>
            <img src="${img}" title="${document.image.fileName}" alt="${document.image.fileName}"/>
            <figcaption>${document.image.description}</figcaption>
        </figure>
    </#if>
    <@hst.html hippohtml=document.content/>
</article>
</#if>